package com.paywithease.payout.application;

import com.paywithease.payout.domain.Beneficiary;
import com.paywithease.payout.domain.RiskLevel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Classifies a payout's risk. HIGH (→ requires maker-checker approval + step-up auth) when the
 * amount exceeds the auto-approve threshold, or the beneficiary was verified/changed within the
 * cooldown window (the "vendor bank change before payout" control), or the destination is unknown.
 */
@Component
public class RiskAssessor {

  private final Clock clock;
  private final long autoApproveThresholdMinor;
  private final Duration newBeneficiaryCooldown;

  public RiskAssessor(
      Clock clock,
      @Value("${peb.payout.auto-approve-threshold-minor:5000000}") long autoApproveThresholdMinor,
      @Value("${peb.payout.new-beneficiary-cooldown-hours:24}") long cooldownHours) {
    this.clock = clock;
    this.autoApproveThresholdMinor = autoApproveThresholdMinor;
    this.newBeneficiaryCooldown = Duration.ofHours(cooldownHours);
  }

  public RiskLevel assess(long amountMinor, Beneficiary beneficiary) {
    if (beneficiary == null) {
      return RiskLevel.HIGH;
    }
    if (amountMinor > autoApproveThresholdMinor) {
      return RiskLevel.HIGH;
    }
    Instant verifiedAt = beneficiary.getVerifiedAt();
    if (verifiedAt == null) {
      return RiskLevel.HIGH; // never verified
    }
    boolean recentlyChanged =
        Duration.between(verifiedAt, clock.instant()).compareTo(newBeneficiaryCooldown) < 0;
    return recentlyChanged ? RiskLevel.HIGH : RiskLevel.LOW;
  }

  public long autoApproveThresholdMinor() {
    return autoApproveThresholdMinor;
  }
}
