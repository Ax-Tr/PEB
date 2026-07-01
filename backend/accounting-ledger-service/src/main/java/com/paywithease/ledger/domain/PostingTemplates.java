package com.paywithease.ledger.domain;

import java.time.LocalDate;

/**
 * Pure functions mapping business events to balanced journals
 * (specs/accounting-chart-of-accounts.md §3). Each returns a {@link JournalCommand} whose {@code
 * build()} asserts Σdebit = Σcredit, so an unbalanced template can never be produced. More
 * templates (purchase, vendor payment, salary, etc.) are added as their source events land in later
 * sprints.
 */
public final class PostingTemplates {

  private PostingTemplates() {}

  /**
   * Customer invoice: Receivable Dr (gross); Sales Revenue Cr (net); Output GST Cr (tax). Under
   * reverse charge / exempt / bill-of-supply, {@code taxMinor} is 0 and no GST line is booked.
   */
  public static JournalCommand customerInvoice(
      LocalDate date,
      long grossMinor,
      long taxMinor,
      String sourceService,
      String sourceEventId,
      String correlationId,
      String narration) {
    long netMinor = grossMinor - taxMinor;
    return JournalCommand.builder(date, narration)
        .source(sourceService, sourceEventId)
        .correlationId(correlationId)
        .debit(Accounts.ACCOUNTS_RECEIVABLE, grossMinor, "Customer receivable")
        .credit(Accounts.SALES_REVENUE, netMinor, "Sales")
        .credit(Accounts.OUTPUT_GST, taxMinor, "Output GST")
        .build();
  }

  /** Customer payment: Bank/UPI/Cash Dr; Customer Receivable Cr. */
  public static JournalCommand customerPayment(
      LocalDate date,
      long amountMinor,
      String cashOrBankAccountCode,
      String sourceService,
      String sourceEventId,
      String correlationId,
      String narration) {
    return JournalCommand.builder(date, narration)
        .source(sourceService, sourceEventId)
        .correlationId(correlationId)
        .debit(cashOrBankAccountCode, amountMinor, "Money in")
        .credit(Accounts.ACCOUNTS_RECEIVABLE, amountMinor, "Settle receivable")
        .build();
  }
}
