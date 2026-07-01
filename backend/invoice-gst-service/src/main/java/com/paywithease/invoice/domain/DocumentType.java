package com.paywithease.invoice.domain;

/** GST document types issued by the service; notes reference an original document. */
public enum DocumentType {
  TAX_INVOICE,
  BILL_OF_SUPPLY,
  RECEIPT_VOUCHER,
  CREDIT_NOTE,
  DEBIT_NOTE;

  public boolean isNote() {
    return this == CREDIT_NOTE || this == DEBIT_NOTE;
  }

  public static boolean isValid(String value) {
    if (value == null) {
      return false;
    }
    for (DocumentType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
