package com.paywithease.reconciliation.domain;

/** Which side of the reconciliation an item belongs to. */
public enum ReconSide {
  EXTERNAL, // imported bank/UPI/settlement rows — the source of truth
  INTERNAL // the business's own records (payments/invoices/payouts/payroll)
}
