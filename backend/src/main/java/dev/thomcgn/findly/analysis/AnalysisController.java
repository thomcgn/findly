package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.common.validation.ListingUrl;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalysisController {

  private final AnalysisService analysisService;

  public AnalysisController(AnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  @PostMapping("/analyses")
  public ResponseEntity<AnalysisStartResponse> createAnalysis(
      @Valid @RequestBody CreateAnalysisRequest request) {
    AnalysisStartResponse response =
        analysisService.createAnalysis(ListingUrl.parse(request.url()).toString());
    return ResponseEntity.accepted().body(response);
  }

  @GetMapping("/analyses/{id}")
  public ResponseEntity<AnalysisStatusResponse> getAnalysisStatus(@PathVariable UUID id) {
    return ResponseEntity.ok(analysisService.getAnalysisStatus(id));
  }

  @GetMapping("/analyses/{id}/result")
  public ResponseEntity<AnalysisDetailResponse> getAnalysisResult(@PathVariable UUID id) {
    return ResponseEntity.ok(analysisService.getAnalysis(id));
  }
}
