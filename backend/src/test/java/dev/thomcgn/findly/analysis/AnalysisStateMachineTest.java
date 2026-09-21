package dev.thomcgn.findly.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AnalysisStateMachineTest {
  @Test
  void allowsOnlyThePipelineAndFailureTransitions() {
    var states = AnalysisStatus.values();
    for (int i = 0; i < states.length; i++) {
      for (int j = 0; j < states.length; j++) {
        boolean expected =
            !states[i].terminal() && (states[j] == AnalysisStatus.FAILED || j == i + 1);
        assertEquals(
            expected, states[i].canTransitionTo(states[j]), states[i] + " -> " + states[j]);
      }
    }
  }
}
