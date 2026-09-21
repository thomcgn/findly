package dev.thomcgn.findly.analysis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnalysisServiceContractTest {
  private final AnalysisRepository repository = mock(AnalysisRepository.class);
  private final AnalysisProcessingService processor = mock(AnalysisProcessingService.class);
  private final AnalysisService service =
      new AnalysisService(repository, processor, mock(AnalysisStatusService.class));

  @Test
  void rejectsUrlBeforePersistingOrScheduling() {
    assertThrows(AnalysisException.class, () -> service.createAnalysis("https://example.com/a"));
    verifyNoInteractions(repository, processor);
  }

  @ParameterizedTest
  @CsvSource({
    "CREATED,,RESULT_NOT_READY",
    "FETCHING_LISTING,,RESULT_NOT_READY",
    "FAILED,,ANALYSIS_FAILED",
    "FAILED,INTERNAL_SECRET,ANALYSIS_FAILED",
    "FAILED,LISTING_FETCH_FAILED,LISTING_FETCH_FAILED",
    "FAILED,ANALYSIS_TIMEOUT,ANALYSIS_TIMEOUT",
    "COMPLETED,,INTERNAL_ERROR"
  })
  void resultUsesPersistedState(
      AnalysisStatus status, String storedCode, AnalysisErrorCode expected) {
    UUID id = UUID.randomUUID();
    when(repository.findById(id))
        .thenReturn(
            Optional.of(
                Analysis.builder()
                    .id(id)
                    .status(status)
                    .errorCode(storedCode)
                    .errorMessage("secret")
                    .build()));
    assertEquals(
        expected, assertThrows(AnalysisException.class, () -> service.getAnalysis(id)).code());
    verifyNoInteractions(processor);
  }

  @Test
  void statusDoesNotExposePersistedInternalMessages() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id))
        .thenReturn(
            Optional.of(
                Analysis.builder()
                    .id(id)
                    .status(AnalysisStatus.FAILED)
                    .errorCode("ANALYSIS_FAILED")
                    .errorMessage("jdbc:postgresql://internal-db?password=secret")
                    .build()));
    var status = service.getAnalysisStatus(id);
    assertEquals("ANALYSIS_FAILED", status.errorCode());
    assertEquals(AnalysisErrorCode.ANALYSIS_FAILED.detail(), status.errorMessage());
  }

  @Test
  void legacySyntheticResultsAreNotPresentedAsEvidence() {
    UUID id = UUID.randomUUID();
    Analysis legacy =
        Analysis.builder()
            .id(id)
            .status(AnalysisStatus.COMPLETED)
            .warnings(java.util.List.of("LEGACY_RESULT_UNVERIFIED"))
            .listing(
                dev.thomcgn.findly.listing.Listing.builder()
                    .title("Legacy")
                    .listingPrice(java.math.BigDecimal.valueOf(149))
                    .currency("EUR")
                    .externalUrl("https://kleinanzeigen.de/legacy")
                    .build())
            .identifiedProduct(
                dev.thomcgn.findly.product.IdentifiedProduct.builder()
                    .model("Invented model")
                    .build())
            .priceSources(
                java.util.List.of(
                    dev.thomcgn.findly.price.PriceSource.builder()
                        .price(java.math.BigDecimal.valueOf(171.35))
                        .build()))
            .build();
    when(repository.findById(id)).thenReturn(Optional.of(legacy));
    var result = service.getAnalysis(id);
    assertNull(result.product());
    assertNull(result.listing().price());
    assertNull(result.market().medianPrice());
    assertNull(result.deal().score());
    assertTrue(result.warnings().contains("LEGACY_RESULT_UNVERIFIED"));
  }
}
