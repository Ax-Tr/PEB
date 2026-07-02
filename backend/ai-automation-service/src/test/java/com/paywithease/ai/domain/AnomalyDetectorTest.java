package com.paywithease.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnomalyDetectorTest {

  @Test
  void flagsAmountFarOutsideHistoricalRange() {
    var history = List.of(10_000L, 11_000L, 9_500L, 10_500L, 10_200L);
    var result = AnomalyDetector.evaluate(history, 500_000L);
    assertThat(result.anomaly()).isTrue();
    assertThat(result.severity()).isEqualTo(AnomalyDetector.Severity.HIGH);
  }

  @Test
  void normalAmountIsNotFlagged() {
    var history = List.of(10_000L, 11_000L, 9_500L, 10_500L, 10_200L);
    var result = AnomalyDetector.evaluate(history, 10_300L);
    assertThat(result.anomaly()).isFalse();
  }

  @Test
  void insufficientHistoryIsNotJudged() {
    var result = AnomalyDetector.evaluate(List.of(10_000L, 11_000L), 500_000L);
    assertThat(result.anomaly()).isFalse();
    assertThat(result.detail()).contains("Insufficient history");
  }

  @Test
  void constantHistoryFallsBackGracefully() {
    var history = List.of(10_000L, 10_000L, 10_000L, 10_000L);
    // MAD is zero and stddev is zero → cannot form a z-score → not an anomaly by score.
    assertThat(AnomalyDetector.evaluate(history, 10_000L).anomaly()).isFalse();
  }
}
