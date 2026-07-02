package com.paywithease.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfidencePolicyTest {

  private final ConfidencePolicy policy = ConfidencePolicy.DEFAULT;

  @Test
  void highConfidenceAutoAppliesAutoApplicableKind() {
    assertThat(policy.decide(SuggestionKind.TRANSACTION_CATEGORY, 0.95))
        .isEqualTo(ConfidencePolicy.Decision.AUTO_APPLY);
  }

  @Test
  void mediumConfidenceNeedsReview() {
    assertThat(policy.decide(SuggestionKind.TRANSACTION_CATEGORY, 0.6))
        .isEqualTo(ConfidencePolicy.Decision.NEEDS_REVIEW);
  }

  @Test
  void lowConfidenceIsRejected() {
    assertThat(policy.decide(SuggestionKind.TRANSACTION_CATEGORY, 0.2))
        .isEqualTo(ConfidencePolicy.Decision.REJECT);
  }

  @Test
  void statutoryNeverAutoAppliesEvenAtFullConfidence() {
    assertThat(policy.decide(SuggestionKind.STATUTORY_FILING, 1.0))
        .isEqualTo(ConfidencePolicy.Decision.NEEDS_REVIEW);
  }

  @Test
  void bankDetailExtractionAlwaysNeedsReview() {
    // OCR of bank details must be user-reviewed before saving — never auto-applied.
    assertThat(policy.decide(SuggestionKind.BANK_DETAIL_EXTRACTION, 0.99))
        .isEqualTo(ConfidencePolicy.Decision.NEEDS_REVIEW);
  }

  @Test
  void cashflowForecastIsAdvisoryOnlyNeverAutoApplied() {
    assertThat(policy.decide(SuggestionKind.CASHFLOW_FORECAST, 0.99))
        .isEqualTo(ConfidencePolicy.Decision.NEEDS_REVIEW);
  }
}
