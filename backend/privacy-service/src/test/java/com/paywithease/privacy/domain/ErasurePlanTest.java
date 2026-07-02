package com.paywithease.privacy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErasurePlanTest {

  @Test
  void financialAndTaxRecordsAreRetainedNotDeleted() {
    var plan =
        ErasurePlan.build(
            List.of(DataCategory.FINANCIAL_TXN, DataCategory.TAX_RECORD, DataCategory.KYC_PII));
    assertThat(plan.fullErasurePossible()).isFalse();
    assertThat(plan.lines())
        .allSatisfy(
            l -> assertThat(l.action()).isEqualTo(RetentionPolicy.Action.RETAIN_LEGAL_HOLD));
    assertThat(plan.summary()).contains("cannot be deleted");
  }

  @Test
  void marketingOnlyCanBeFullyDeleted() {
    var plan = ErasurePlan.build(List.of(DataCategory.MARKETING));
    assertThat(plan.fullErasurePossible()).isTrue();
    assertThat(plan.lines().get(0).action()).isEqualTo(RetentionPolicy.Action.DELETE);
  }

  @Test
  void mixedSetProducesPartialPlan() {
    var plan =
        ErasurePlan.build(
            List.of(DataCategory.MARKETING, DataCategory.CONTACT_PII, DataCategory.FINANCIAL_TXN));
    assertThat(plan.fullErasurePossible()).isFalse();
    assertThat(plan.lines())
        .extracting(ErasurePlan.Line::action)
        .containsExactlyInAnyOrder(
            RetentionPolicy.Action.DELETE,
            RetentionPolicy.Action.ANONYMIZE,
            RetentionPolicy.Action.RETAIN_LEGAL_HOLD);
  }

  @Test
  void deduplicatesCategories() {
    var plan = ErasurePlan.build(List.of(DataCategory.MARKETING, DataCategory.MARKETING));
    assertThat(plan.lines()).hasSize(1);
  }
}
