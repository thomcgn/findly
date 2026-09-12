package dev.thomcgn.findly.analysis;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class AnalysisController {

  private final AnalysisService analysisService;

  public AnalysisController(AnalysisService analysisService) {
    this.analysisService = analysisService;
  }

  @PostMapping("/analyses")
  public ResponseEntity<AnalysisStartResponse> createAnalysis(
      @Valid @RequestBody CreateAnalysisRequest request) {
    AnalysisStartResponse response = analysisService.createAnalysis(request.url());
    return ResponseEntity.accepted().body(response);
  }

  @GetMapping("/analyses/{id}")
  public ResponseEntity<AnalysisDetailResponse> getAnalysis(@PathVariable UUID id) {
    return ResponseEntity.ok(analysisService.getAnalysis(id));
  }
}
