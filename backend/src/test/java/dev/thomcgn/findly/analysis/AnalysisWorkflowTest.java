package dev.thomcgn.findly.analysis;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import dev.thomcgn.findly.listing.ListingFetchResult;
import dev.thomcgn.findly.listing.ListingTransport;
import dev.thomcgn.findly.provider.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockitoBean(
    types = {
      ListingTransport.class,
      OcrProvider.class,
      VisionProvider.class,
      SearchProvider.class,
      PricingProvider.class
    })
class AnalysisWorkflowTest {
  private final AnalysisRepository repository;
  private final AnalysisStatusService states;
  private final AnalysisService service;
  private final ListingTransport transport;
  private final OcrProvider ocr;
  private final VisionProvider vision;
  private final SearchProvider search;
  private final PricingProvider pricing;
  private final MockMvc mvc;
  private final ObjectMapper json;

  @Autowired
  AnalysisWorkflowTest(
      AnalysisRepository repository,
      AnalysisStatusService states,
      AnalysisService service,
      ListingTransport transport,
      OcrProvider ocr,
      VisionProvider vision,
      SearchProvider search,
      PricingProvider pricing,
      MockMvc mvc,
      ObjectMapper json) {
    this.repository = repository;
    this.states = states;
    this.service = service;
    this.transport = transport;
    this.ocr = ocr;
    this.vision = vision;
    this.search = search;
    this.pricing = pricing;
    this.mvc = mvc;
    this.json = json;
  }

  @BeforeEach
  void fixtures() {
    when(ocr.extract(any()))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              return ProviderResult.unavailable();
            });
    when(vision.analyze(any()))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              return ProviderResult.unavailable();
            });
    when(search.search(any(), any()))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              return ProviderResult.unavailable();
            });
  }

  @Test
  void returns202BeforeNetworkCompletesAndPersistsAllStagesWithoutProviderTransactions()
      throws Exception {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(transport.get(any()))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              entered.countDown();
              assertTrue(release.await(10, TimeUnit.SECONDS));
              return new ListingTransport.Response(200, null, "<h1 id=viewad-title>Fixture</h1>");
            });
    UUID id;
    try {
      var response =
          assertTimeoutPreemptively(
              Duration.ofSeconds(3),
              () ->
                  mvc.perform(
                          post("/api/analyses")
                              .contentType("application/json")
                              .content("{\"url\":\"https://kleinanzeigen.de/fixture\"}"))
                      .andExpect(status().isAccepted())
                      .andExpect(jsonPath("$.status").value("CREATED"))
                      .andReturn()
                      .getResponse());
      id = UUID.fromString(json.readTree(response.getContentAsString()).get("id").asString());
      assertTrue(entered.await(3, TimeUnit.SECONDS));
      var pending = service.getAnalysisStatus(id);
      assertEquals(AnalysisStatus.FETCHING_LISTING, pending.status());
      assertEquals(10, pending.progress());
      assertNotNull(pending.startedAt());
      mvc.perform(get("/api/analyses/{id}/result", id)).andExpect(status().isConflict());
    } finally {
      release.countDown();
    }
    await()
        .atMost(Duration.ofSeconds(10))
        .until(() -> service.getAnalysisStatus(id).status().terminal());
    assertEquals(AnalysisStatus.COMPLETED, service.getAnalysisStatus(id).status());
    assertNull(service.getAnalysis(id).listing().price());
    assertNull(service.getAnalysis(id).deal().score());
    assertEquals(100, service.getAnalysisStatus(id).progress());
    verify(transport, times(1)).get(any());
    verify(ocr, times(1)).extract(any());
    verify(vision, times(1)).analyze(any());
    verify(search, times(1)).search(any(), any());
    verifyNoInteractions(pricing);
    var result = service.getAnalysis(id);
    assertNull(result.product());
    assertNull(result.market().medianPrice());
    assertTrue(
        result
            .warnings()
            .containsAll(
                List.of(
                    "OCR_UNAVAILABLE",
                    "VISION_UNAVAILABLE",
                    "SEARCH_UNAVAILABLE",
                    "PRODUCT_NOT_IDENTIFIED",
                    "PRICE_NOT_VERIFIED")));
    assertEquals(result.warnings(), service.getAnalysisStatus(id).warnings());
    states.fail(id, AnalysisErrorCode.ANALYSIS_TIMEOUT);
    assertEquals(AnalysisStatus.COMPLETED, service.getAnalysisStatus(id).status());
  }

  @Test
  void persistsCandidateScoresEvidenceAndSeparateVerifiedComparisons() throws Exception {
    var evidence =
        List.of(
            new ProductEvidence(
                "model",
                "Fixture 12",
                java.net.URI.create("https://fixture.test/spec"),
                Instant.now().minusSeconds(5)));
    var candidate =
        new ProductCandidate(
            "Fixture 12", "Fixture", "Fixture 12", "Chair", java.util.Map.of(), evidence);
    when(transport.get(any()))
        .thenReturn(
            new ListingTransport.Response(
                200, null, "<h1 id=viewad-title>Fixture 12</h1><h2 id=viewad-price>120 €</h2>"));
    when(ocr.extract(any()))
        .thenReturn(new ProviderResult<>(ProviderResult.Availability.AVAILABLE, evidence));
    when(search.search(any(), any()))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              return new ProviderResult<>(
                  ProviderResult.Availability.AVAILABLE, List.of(candidate));
            });
    var used =
        new PriceQuote(
            new java.math.BigDecimal("100"),
            "EUR",
            "Fixture marketplace",
            java.net.URI.create("https://fixture.test/used"),
            Instant.now().minusSeconds(5),
            PriceQuote.Kind.CURRENT_USED,
            new java.math.BigDecimal(".9"),
            PriceQuote.SourceType.MARKETPLACE);
    var original =
        new PriceQuote(
            new java.math.BigDecimal("200"),
            "EUR",
            "Fixture manufacturer",
            java.net.URI.create("https://fixture.test/new"),
            Instant.now().minusSeconds(5),
            PriceQuote.Kind.ORIGINAL,
            new java.math.BigDecimal(".9"),
            PriceQuote.SourceType.MANUFACTURER);
    var dollars =
        new PriceQuote(
            new java.math.BigDecimal("20"),
            "USD",
            "Fixture marketplace",
            java.net.URI.create("https://fixture.test/us"),
            Instant.now().minusSeconds(5),
            PriceQuote.Kind.CURRENT_USED,
            new java.math.BigDecimal(".9"),
            PriceQuote.SourceType.MARKETPLACE);
    when(pricing.research(any()))
        .thenAnswer(
            invocation -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              return new ProviderResult<>(
                  ProviderResult.Availability.AVAILABLE, List.of(used, original, dollars));
            });
    UUID id = service.createAnalysis("https://kleinanzeigen.de/evidence").id();
    await()
        .atMost(Duration.ofSeconds(10))
        .until(() -> service.getAnalysisStatus(id).status().terminal());
    assertEquals(AnalysisStatus.COMPLETED, service.getAnalysisStatus(id).status());
    var result = service.getAnalysis(id);
    assertEquals("Fixture 12", result.product().model());
    assertEquals(1, result.candidates().size());
    assertEquals(evidence, result.attributes());
    assertEquals(3, result.priceEvidence().size());
    assertEquals(2, result.comparisons().size());
    var comparison =
        result.comparisons().stream()
            .filter(c -> c.kind() == PriceQuote.Kind.CURRENT_USED)
            .findFirst()
            .orElseThrow();
    assertEquals(0, new java.math.BigDecimal("-20").compareTo(comparison.savings()));
    assertEquals("SURCHARGE", comparison.outcome());
    assertFalse(result.warnings().contains("PRODUCT_NOT_IDENTIFIED"));
    assertFalse(result.warnings().contains("PRICE_NOT_VERIFIED"));
    mvc.perform(get("/api/analyses/{id}/result", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.candidates[0].components.model").value(1))
        .andExpect(jsonPath("$.priceEvidence.length()").value(3));
  }

  @Test
  void providerFailureIsPersistedWithStableCode() {
    when(transport.get(any()))
        .thenThrow(new AnalysisException(AnalysisErrorCode.LISTING_ACCESS_BLOCKED));
    UUID id = service.createAnalysis("https://kleinanzeigen.de/blocked").id();
    await()
        .atMost(Duration.ofSeconds(10))
        .until(() -> service.getAnalysisStatus(id).status().terminal());
    assertEquals("LISTING_ACCESS_BLOCKED", service.getAnalysisStatus(id).errorCode());
    assertNotNull(service.getAnalysisStatus(id).failedAt());
    verifyNoInteractions(ocr, vision, search, pricing);
  }

  @Test
  void onlyOneWorkerCanClaimTheSameCommittedAnalysis() throws Exception {
    var job = states.create("https://kleinanzeigen.de/claim");
    try (var executor = Executors.newFixedThreadPool(4)) {
      var jobs =
          java.util.stream.IntStream.range(0, 8)
              .mapToObj(i -> (Callable<Boolean>) () -> states.claim(job.id()))
              .toList();
      int claims = 0;
      for (var result : executor.invokeAll(jobs)) if (result.get()) claims++;
      assertEquals(1, claims);
    }
    states.fail(job.id(), AnalysisErrorCode.ANALYSIS_FAILED);
  }

  @Test
  void expiredJobsSurviveRestartAndLateResultsCannotOverwriteFailure() {
    var job = states.create("https://kleinanzeigen.de/expired");
    assertEquals(
        300,
        Duration.between(repository.findById(job.id()).orElseThrow().getCreatedAt(), job.deadline())
            .toSeconds(),
        1);
    assertTrue(states.claim(job.id()));
    states.extracting(job.id());
    Analysis analysis = repository.findById(job.id()).orElseThrow();
    analysis.setDeadlineAt(Instant.now().minusSeconds(1));
    repository.saveAndFlush(analysis);
    states.expireOverdue();
    assertEquals("ANALYSIS_TIMEOUT", service.getAnalysisStatus(job.id()).errorCode());
    assertThrows(
        AnalysisException.class,
        () ->
            states.storeListing(
                job.id(), new ListingFetchResult("Late", null, null, null, List.of())));
    assertEquals(AnalysisStatus.FAILED, service.getAnalysisStatus(job.id()).status());
    assertFalse(states.claim(job.id()));
  }

  @Test
  void cannotSkipStagesOrCompleteWithoutResults() {
    var job = states.create("https://kleinanzeigen.de/invalid");
    assertThrows(IllegalStateException.class, () -> states.extracting(job.id()));
    assertThrows(
        IllegalStateException.class,
        () ->
            states.complete(
                job.id(), new dev.thomcgn.findly.price.PriceResearchResult(List.of(), List.of())));
    assertEquals(AnalysisStatus.CREATED, service.getAnalysisStatus(job.id()).status());
    states.fail(job.id(), AnalysisErrorCode.ANALYSIS_FAILED);
  }
}
