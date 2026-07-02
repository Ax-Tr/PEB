package com.paywithease.installment.domain;

import java.time.LocalDate;

/** EMI cadence. */
public enum Frequency {
  WEEKLY,
  FORTNIGHTLY,
  MONTHLY;

  public LocalDate advance(LocalDate from, int periods) {
    return switch (this) {
      case WEEKLY -> from.plusWeeks(periods);
      case FORTNIGHTLY -> from.plusWeeks(2L * periods);
      case MONTHLY -> from.plusMonths(periods);
    };
  }

  public static Frequency of(String value) {
    for (Frequency f : values()) {
      if (f.name().equals(value)) {
        return f;
      }
    }
    return MONTHLY;
  }
}
