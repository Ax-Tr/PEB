package com.paywithease.ocr.domain;

public enum DocumentType {
  BANK_DETAILS,
  CHEQUE,
  PASSBOOK,
  INVOICE,
  RECEIPT;

  public static boolean isValid(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    for (DocumentType type : values()) {
      if (type.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
