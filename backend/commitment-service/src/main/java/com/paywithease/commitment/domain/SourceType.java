package com.paywithease.commitment.domain;

public enum SourceType {
  MANUAL,
  INVOICE,
  PAYMENT_REQUEST,
  INSTALLMENT,
  VOICE,
  OCR_NOTE;

  public static boolean isValid(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    for (SourceType type : values()) {
      if (type.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
