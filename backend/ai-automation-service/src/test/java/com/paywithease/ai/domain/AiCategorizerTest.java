package com.paywithease.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiCategorizerTest {

  @Test
  void classifiesKnownNarrationsWithConfidence() {
    var salary = AiCategorizer.classify("Monthly SALARY payout to staff");
    assertThat(salary.category()).isEqualTo("SALARY");
    assertThat(salary.confidence()).isGreaterThan(0.9);

    assertThat(AiCategorizer.classify("Airtel postpaid recharge").category()).isEqualTo("TELECOM");
    assertThat(AiCategorizer.classify("CGST + SGST payment").category()).isEqualTo("GST_PAYMENT");
  }

  @Test
  void unknownNarrationIsLowConfidenceUncategorised() {
    var c = AiCategorizer.classify("xyzzy random text");
    assertThat(c.category()).isEqualTo("UNCATEGORISED");
    assertThat(c.confidence()).isLessThan(0.5);
  }

  @Test
  void blankNarrationIsZeroConfidence() {
    assertThat(AiCategorizer.classify("  ").confidence()).isZero();
  }
}
