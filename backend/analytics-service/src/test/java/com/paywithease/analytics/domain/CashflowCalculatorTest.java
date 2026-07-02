package com.paywithease.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CashflowCalculatorTest {

  @Test
  void carriesRunningBalanceAcrossPeriods() {
    var cf =
        CashflowCalculator.compute(
            100_000,
            List.of(
                new CashflowCalculator.PeriodFlow(2026, 5, 500_000, 300_000),
                new CashflowCalculator.PeriodFlow(2026, 6, 200_000, 250_000)));
    assertThat(cf.totalInflowMinor()).isEqualTo(700_000);
    assertThat(cf.totalOutflowMinor()).isEqualTo(550_000);
    assertThat(cf.netMinor()).isEqualTo(150_000);
    assertThat(cf.closingBalanceMinor()).isEqualTo(250_000); // 100000 + 200000 - 50000
    assertThat(cf.periods().get(0).closingBalanceMinor()).isEqualTo(300_000);
    assertThat(cf.periods().get(1).netMinor()).isEqualTo(-50_000);
  }

  @Test
  void emptyPeriodsReturnsOpeningAsClosing() {
    var cf = CashflowCalculator.compute(75_000, List.of());
    assertThat(cf.closingBalanceMinor()).isEqualTo(75_000);
    assertThat(cf.netMinor()).isZero();
    assertThat(cf.periods()).isEmpty();
  }
}
