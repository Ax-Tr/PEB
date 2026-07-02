package com.paywithease.installment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmiScheduleGeneratorTest {

  private static final LocalDate START = LocalDate.of(2026, 6, 1);

  @Test
  void evenSplitSumsToPrincipal() {
    List<EmiScheduleGenerator.PlannedEmi> emis =
        EmiScheduleGenerator.generate(120000, 3, START, Frequency.MONTHLY);
    assertThat(emis).hasSize(3);
    assertThat(emis).allMatch(e -> e.amountMinor() == 40000);
    assertThat(total(emis)).isEqualTo(120000);
  }

  @Test
  void unevenSplitDistributesRemainderAndSumsExactly() {
    // 100 paise over 3 EMIs -> 34, 33, 33 (remainder 1 to the earliest)
    List<EmiScheduleGenerator.PlannedEmi> emis =
        EmiScheduleGenerator.generate(100, 3, START, Frequency.MONTHLY);
    assertThat(emis.get(0).amountMinor()).isEqualTo(34);
    assertThat(emis.get(1).amountMinor()).isEqualTo(33);
    assertThat(emis.get(2).amountMinor()).isEqualTo(33);
    assertThat(total(emis)).isEqualTo(100); // exact, no paise lost
  }

  @Test
  void monthlyDueDatesAdvance() {
    List<EmiScheduleGenerator.PlannedEmi> emis =
        EmiScheduleGenerator.generate(300, 3, START, Frequency.MONTHLY);
    assertThat(emis.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    assertThat(emis.get(1).dueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    assertThat(emis.get(2).dueDate()).isEqualTo(LocalDate.of(2026, 8, 1));
  }

  @Test
  void weeklyDueDatesAdvance() {
    List<EmiScheduleGenerator.PlannedEmi> emis =
        EmiScheduleGenerator.generate(200, 2, START, Frequency.WEEKLY);
    assertThat(emis.get(1).dueDate()).isEqualTo(LocalDate.of(2026, 6, 8));
  }

  @Test
  void rejectsInvalidInputs() {
    assertThatThrownBy(() -> EmiScheduleGenerator.generate(0, 3, START, Frequency.MONTHLY))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> EmiScheduleGenerator.generate(100, 0, START, Frequency.MONTHLY))
        .isInstanceOf(ApiException.class);
  }

  private static long total(List<EmiScheduleGenerator.PlannedEmi> emis) {
    return emis.stream().mapToLong(EmiScheduleGenerator.PlannedEmi::amountMinor).sum();
  }
}
