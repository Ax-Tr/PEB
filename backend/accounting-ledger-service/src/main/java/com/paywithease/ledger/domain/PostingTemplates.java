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

  /**
   * Vendor purchase bill: Purchase/Expense Dr (net); Input GST Dr (ITC); Vendor Payable Cr (gross).
   * Under reverse charge {@code inputGstMinor} is 0 here (RCM ITC is handled in the GST return).
   */
  public static JournalCommand vendorPurchase(
      LocalDate date,
      long netMinor,
      long inputGstMinor,
      String sourceService,
      String sourceEventId,
      String correlationId,
      String narration) {
    long gross = netMinor + inputGstMinor;
    return JournalCommand.builder(date, narration)
        .source(sourceService, sourceEventId)
        .correlationId(correlationId)
        .debit(Accounts.COGS, netMinor, "Purchase")
        .debit(Accounts.INPUT_GST, inputGstMinor, "Input GST (ITC)")
        .credit(Accounts.ACCOUNTS_PAYABLE, gross, "Vendor payable")
        .build();
  }

  /** Vendor payment: Vendor Payable Dr; Bank/UPI/Cash Cr. */
  public static JournalCommand vendorPayment(
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
        .debit(Accounts.ACCOUNTS_PAYABLE, amountMinor, "Settle payable")
        .credit(cashOrBankAccountCode, amountMinor, "Money out")
        .build();
  }

  /**
   * Salary run: Salary Expense Dr (total earnings); Employee Payable Cr (net pay); Statutory
   * Payable Cr (PF+ESI+PT+other withholdings); TDS Payable Cr (salary TDS). Balances because {@code
   * totalEarnings = net + statutoryWithheld + tds}.
   */
  public static JournalCommand salaryRun(
      LocalDate date,
      long totalEarningsMinor,
      long netPayMinor,
      long statutoryWithheldMinor,
      long tdsMinor,
      String sourceService,
      String sourceEventId,
      String correlationId,
      String narration) {
    return JournalCommand.builder(date, narration)
        .source(sourceService, sourceEventId)
        .correlationId(correlationId)
        .debit(Accounts.SALARY_EXPENSE, totalEarningsMinor, "Salary & wages")
        .credit(Accounts.EMPLOYEE_PAYABLE, netPayMinor, "Net salary payable")
        .credit(Accounts.STATUTORY_PAYABLE, statutoryWithheldMinor, "PF/ESI/PT withheld")
        .credit(Accounts.TDS_PAYABLE, tdsMinor, "Salary TDS")
        .build();
  }
}
