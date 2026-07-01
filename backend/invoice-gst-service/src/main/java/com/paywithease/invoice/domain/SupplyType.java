package com.paywithease.invoice.domain;

/** GST supply classification: business-to-business or business-to-consumer. */
public enum SupplyType {
  B2B,
  B2C;

  public static boolean isValid(String value) {
    if (value == null) {
      return false;
    }
    for (SupplyType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
