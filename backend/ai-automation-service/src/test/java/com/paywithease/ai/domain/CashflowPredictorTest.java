package com.paywithease.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CashflowPredictorTest {

  @Test
  void projectsRisingTrendWithHighConfidence() {
    var forecast = CashflowPredictor.predict(List.of(100_000L, 200_000L, 300_000L, 400_000L));
    assertThat(forecast.projectedNetMinor()).isEqualTo(500_000L);
    assertThat(forecast.confidence()).isGreaterThan(0.99); // perfectly linear
  }

  @Test
  void noisySeriesHasLowerConfidence() {
    var forecast =
        CashflowPredictor.predict(List.of(100_000L, 500_000L, 120_000L, 480_000L, 90_000L));
    assertThat(forecast.confidence()).isLessThan(0.5);
  }

  @Test
  void insufficientHistoryReturnsZeroConfidence() {
    var forecast = CashflowPredictor.predict(List.of(100_000L, 200_000L));
    assertThat(forecast.confidence()).isZero();
  }
}
