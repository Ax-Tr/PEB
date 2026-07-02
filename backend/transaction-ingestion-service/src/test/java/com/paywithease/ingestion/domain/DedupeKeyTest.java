package com.paywithease.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DedupeKeyTest {

  private static final LocalDate D = LocalDate.of(2026, 7, 1);

  @Test
  void sameExternalRefProducesSameKey() {
    String a =
        DedupeKey.compute(
            "t1", "acc1", TxnSource.BANK_IMPORT, Direction.CREDIT, 100, D, "UTR123", "x");
    String b =
        DedupeKey.compute(
            "t1",
            "acc1",
            TxnSource.BANK_IMPORT,
            Direction.CREDIT,
            999,
            D.plusDays(5),
            "UTR123",
            "y");
    assertThat(a).isEqualTo(b); // external ref dominates → re-import dedupes
    assertThat(a).hasSize(64);
  }

  @Test
  void fallsBackToNaturalKeyWhenNoExternalRef() {
    String a =
        DedupeKey.compute(
            "t1", "acc1", TxnSource.BANK_IMPORT, Direction.DEBIT, 5000, D, null, "Rent paid");
    String same =
        DedupeKey.compute(
            "t1", "acc1", TxnSource.BANK_IMPORT, Direction.DEBIT, 5000, D, null, "RENT   PAID");
    String different =
        DedupeKey.compute(
            "t1", "acc1", TxnSource.BANK_IMPORT, Direction.DEBIT, 5001, D, null, "Rent paid");
    assertThat(a).isEqualTo(same); // narration normalized (case/whitespace)
    assertThat(a).isNotEqualTo(different); // amount differs
  }

  @Test
  void differentTenantsNeverCollide() {
    String t1 =
        DedupeKey.compute(
            "t1", "acc1", TxnSource.UPI_IMPORT, Direction.CREDIT, 100, D, "REF", null);
    String t2 =
        DedupeKey.compute(
            "t2", "acc1", TxnSource.UPI_IMPORT, Direction.CREDIT, 100, D, "REF", null);
    assertThat(t1).isNotEqualTo(t2);
  }
}
