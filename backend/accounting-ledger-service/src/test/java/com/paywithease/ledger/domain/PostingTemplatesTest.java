package com.paywithease.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Hard gate: every posting template must balance, and the invariant must reject imbalance. */
class PostingTemplatesTest {

  private static final LocalDate DATE = LocalDate.of(2026, 5, 15);

  private static void assertBalanced(JournalCommand j) {
    long debit = j.lines().stream().mapToLong(JournalCommand.Line::debitMinor).sum();
    long credit = j.lines().stream().mapToLong(JournalCommand.Line::creditMinor).sum();
    assertThat(debit).as("debits == credits").isEqualTo(credit);
    assertThat(j.lines()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void customerInvoiceBalances() {
    JournalCommand j =
        PostingTemplates.customerInvoice(
            DATE, 118000, 18000, "invoice-gst-service", "e1", "c", "INV1");
    assertBalanced(j);
    // Receivable 118000 Dr; Sales 100000 Cr; Output GST 18000 Cr
    assertThat(lineDebit(j, Accounts.ACCOUNTS_RECEIVABLE)).isEqualTo(118000);
    assertThat(lineCredit(j, Accounts.SALES_REVENUE)).isEqualTo(100000);
    assertThat(lineCredit(j, Accounts.OUTPUT_GST)).isEqualTo(18000);
  }

  @Test
  void customerInvoiceWithoutTaxOmitsGstLine() {
    JournalCommand j =
        PostingTemplates.customerInvoice(DATE, 50000, 0, "invoice-gst-service", "e2", "c", "BOS1");
    assertBalanced(j);
    assertThat(j.lines()).hasSize(2); // Receivable Dr, Sales Cr — no GST line
    assertThat(lineCredit(j, Accounts.OUTPUT_GST)).isZero();
  }

  @Test
  void customerPaymentBalances() {
    JournalCommand j =
        PostingTemplates.customerPayment(
            DATE, 118000, Accounts.UPI_CLEARING, "payment-collection-service", "p1", "c", "PMT1");
    assertBalanced(j);
    assertThat(lineDebit(j, Accounts.UPI_CLEARING)).isEqualTo(118000);
    assertThat(lineCredit(j, Accounts.ACCOUNTS_RECEIVABLE)).isEqualTo(118000);
  }

  @Test
  void unbalancedManualCommandIsRejected() {
    assertThatThrownBy(
            () ->
                new JournalCommand(
                        DATE,
                        "bad",
                        null,
                        null,
                        null,
                        null,
                        List.of(
                            new JournalCommand.Line(Accounts.BANK, 100, 0, "d"),
                            new JournalCommand.Line(Accounts.SALES_REVENUE, 0, 90, "c")))
                    .validateBalanced())
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("do not equal");
  }

  @Test
  void singleLineCommandIsRejected() {
    assertThatThrownBy(
            () -> JournalCommand.builder(DATE, "x").debit(Accounts.BANK, 100, "d").build())
        .isInstanceOf(ApiException.class);
  }

  private static long lineDebit(JournalCommand j, String code) {
    return j.lines().stream()
        .filter(l -> l.accountCode().equals(code))
        .mapToLong(JournalCommand.Line::debitMinor)
        .sum();
  }

  private static long lineCredit(JournalCommand j, String code) {
    return j.lines().stream()
        .filter(l -> l.accountCode().equals(code))
        .mapToLong(JournalCommand.Line::creditMinor)
        .sum();
  }
}
