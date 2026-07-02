package com.paywithease.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FreshnessTest {

  private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

  @Test
  void freshWhenWithinThreshold() {
    var s = Freshness.evaluate(NOW.minusSeconds(30), NOW, Duration.ofSeconds(120));
    assertThat(s.state()).isEqualTo(Freshness.State.FRESH);
    assertThat(s.lagSeconds()).isEqualTo(30);
  }

  @Test
  void staleWhenBeyondThreshold() {
    var s = Freshness.evaluate(NOW.minusSeconds(600), NOW, Duration.ofSeconds(120));
    assertThat(s.state()).isEqualTo(Freshness.State.STALE);
    assertThat(s.lagSeconds()).isEqualTo(600);
  }

  @Test
  void noDataWhenNeverProcessed() {
    var s = Freshness.evaluate(null, NOW, Duration.ofSeconds(120));
    assertThat(s.state()).isEqualTo(Freshness.State.NO_DATA);
  }

  @Test
  void clampsNegativeLagToZero() {
    var s = Freshness.evaluate(NOW.plusSeconds(5), NOW, Duration.ofSeconds(120));
    assertThat(s.lagSeconds()).isZero();
    assertThat(s.state()).isEqualTo(Freshness.State.FRESH);
  }
}
