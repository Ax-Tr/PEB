package com.paywithease.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfitabilityRankerTest {

  @Test
  void ranksByProfitDescendingWithMargin() {
    var ranked =
        ProfitabilityRanker.rank(
            List.of(
                new ProfitabilityRanker.Item("p1", "Widget", 100_000, 90_000),
                new ProfitabilityRanker.Item("p2", "Gadget", 200_000, 50_000),
                new ProfitabilityRanker.Item("p3", "Gizmo", 150_000, 150_000)));
    assertThat(ranked)
        .extracting(ProfitabilityRanker.Ranked::productId)
        .containsExactly("p2", "p1", "p3");
    assertThat(ranked.get(0).profitMinor()).isEqualTo(150_000);
    assertThat(ranked.get(0).marginPct()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(ranked.get(2).profitMinor()).isZero();
  }

  @Test
  void emptyInputReturnsEmpty() {
    assertThat(ProfitabilityRanker.rank(List.of())).isEmpty();
  }
}
