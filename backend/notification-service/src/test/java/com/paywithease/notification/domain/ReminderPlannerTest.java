package com.paywithease.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReminderPlannerTest {

  @Test
  void plansD3D1DdayWhenAllInFuture() {
    LocalDate due = LocalDate.of(2026, 7, 10);
    LocalDate today = LocalDate.of(2026, 7, 1);
    var plans = ReminderPlanner.plan(due, ReminderPlanner.DEFAULT_OFFSETS, today);
    assertThat(plans).hasSize(3);
    assertThat(plans.get(0).sendOn()).isEqualTo(LocalDate.of(2026, 7, 7)); // D-3
    assertThat(plans.get(1).sendOn()).isEqualTo(LocalDate.of(2026, 7, 9)); // D-1
    assertThat(plans.get(2).sendOn()).isEqualTo(LocalDate.of(2026, 7, 10)); // D-day
  }

  @Test
  void skipsPastReminderDates() {
    LocalDate due = LocalDate.of(2026, 7, 2);
    LocalDate today = LocalDate.of(2026, 7, 2); // D-3 and D-1 are in the past
    var plans = ReminderPlanner.plan(due, ReminderPlanner.DEFAULT_OFFSETS, today);
    assertThat(plans).hasSize(1);
    assertThat(plans.get(0).offsetDays()).isZero(); // only D-day remains
  }
}
