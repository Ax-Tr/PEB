package com.paywithease.payout.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywithease.payout.domain.Beneficiary;
import com.paywithease.payout.domain.PartyType;
import com.paywithease.payout.domain.RiskLevel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RiskAssessorTest {

  private static final Instant NOW = Instant.parse("2026-05-15T00:00:00Z");
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  // threshold ₹50,000 = 5,000,000 paise; cooldown 24h
  private final RiskAssessor assessor = new RiskAssessor(clock, 5_000_000, 24);

  private Beneficiary beneficiary(Instant verifiedAt) {
    return new Beneficiary("b1", "t1", PartyType.VENDOR, "v1", "acme", "hash", verifiedAt, NOW);
  }

  @Test
  void nullBeneficiaryIsHighRisk() {
    assertThat(assessor.assess(100, null)).isEqualTo(RiskLevel.HIGH);
  }

  @Test
  void aboveThresholdIsHighRisk() {
    Beneficiary old = beneficiary(NOW.minusSeconds(30 * 24 * 3600)); // verified a month ago
    assertThat(assessor.assess(5_000_001, old)).isEqualTo(RiskLevel.HIGH);
  }

  @Test
  void recentlyVerifiedBeneficiaryIsHighRisk() {
    Beneficiary recent = beneficiary(NOW.minusSeconds(3600)); // verified 1h ago (< 24h cooldown)
    assertThat(assessor.assess(1000, recent)).isEqualTo(RiskLevel.HIGH);
  }

  @Test
  void neverVerifiedBeneficiaryIsHighRisk() {
    assertThat(assessor.assess(1000, beneficiary(null))).isEqualTo(RiskLevel.HIGH);
  }

  @Test
  void establishedBeneficiarySmallAmountIsLowRisk() {
    Beneficiary old = beneficiary(NOW.minusSeconds(30 * 24 * 3600));
    assertThat(assessor.assess(1000, old)).isEqualTo(RiskLevel.LOW);
  }
}
