package com.paywithease.commitment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CommitmentTest {

  private static final Instant NOW = Instant.parse("2026-07-02T00:00:00Z");
  private static final LocalDate DUE = LocalDate.of(2026, 7, 10);

  private Commitment commitment(long amountMinor) {
    return new Commitment(
        "c1",
        "tenant1",
        CounterpartyType.CUSTOMER,
        "cust1",
        "Raj",
        SourceType.MANUAL,
        null,
        "Course fee",
        amountMinor,
        DUE,
        "actor1",
        NOW);
  }

  @Test
  void partialPaymentUpdatesOutstandingAndStatus() {
    Commitment c = commitment(100_000);

    long applied = c.recordPayment(40_000, NOW.plusSeconds(10));

    assertThat(applied).isEqualTo(40_000);
    assertThat(c.getPaidMinor()).isEqualTo(40_000);
    assertThat(c.outstandingMinor()).isEqualTo(60_000);
    assertThat(c.getStatus()).isEqualTo("PARTIALLY_PAID");
  }

  @Test
  void fullPaymentClosesCommitment() {
    Commitment c = commitment(100_000);

    c.recordPayment(150_000, NOW.plusSeconds(10));

    assertThat(c.getPaidMinor()).isEqualTo(100_000);
    assertThat(c.outstandingMinor()).isZero();
    assertThat(c.getStatus()).isEqualTo("PAID");
    assertThat(c.getClosedAt()).isNotNull();
  }

  @Test
  void reschedulePreservesOldDueDateAndStatus() {
    Commitment c = commitment(100_000);

    LocalDate old =
        c.reschedule(DUE.plusDays(5), "Customer asked for more time", NOW.plusSeconds(10));

    assertThat(old).isEqualTo(DUE);
    assertThat(c.getDueDate()).isEqualTo(DUE.plusDays(5));
    assertThat(c.getStatus()).isEqualTo("RESCHEDULED");
  }

  @Test
  void paidCommitmentCannotBeRescheduled() {
    Commitment c = commitment(100_000);
    c.recordPayment(100_000, NOW.plusSeconds(10));

    assertThatThrownBy(() -> c.reschedule(DUE.plusDays(1), null, NOW.plusSeconds(20)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("PAID");
  }
}
