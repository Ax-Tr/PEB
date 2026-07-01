package com.paywithease.ledger.domain;

/** Canonical chart-of-accounts codes (see specs/accounting-chart-of-accounts.md §2). */
public final class Accounts {

  private Accounts() {}

  public static final String CASH = "1000";
  public static final String BANK = "1010";
  public static final String UPI_CLEARING = "1020";
  public static final String ACCOUNTS_RECEIVABLE = "1100";
  public static final String INPUT_GST = "1200";
  public static final String INVENTORY_PURCHASES = "1300";
  public static final String FIXED_ASSETS = "1900";
  public static final String ACCUM_DEPRECIATION = "1910";
  public static final String ACCOUNTS_PAYABLE = "2000";
  public static final String OUTPUT_GST = "2100";
  public static final String EMPLOYEE_PAYABLE = "2200";
  public static final String STATUTORY_PAYABLE = "2210"; // PF/ESI/PT
  public static final String TDS_PAYABLE = "2220";
  public static final String GST_PAYABLE = "2300";
  public static final String LOAN_LIABILITY = "2400";
  public static final String OWNER_CAPITAL = "3000";
  public static final String OWNER_DRAWINGS = "3100";
  public static final String SALES_REVENUE = "4000";
  public static final String OTHER_INCOME = "4100";
  public static final String COGS = "5000";
  public static final String SALARY_EXPENSE = "5100";
  public static final String OFFICE_EXPENSE = "5200";
  public static final String INTEREST_EXPENSE = "5300";
  public static final String DEPRECIATION_EXPENSE = "5400";
}
