package com.paywithease.ingestion.domain;

/** Where a transaction came from. */
public enum TxnSource {
  MANUAL_CASH,
  MANUAL_BANK,
  BANK_IMPORT,
  UPI_IMPORT,
  GATEWAY_SETTLEMENT;

  public static boolean isValid(String value) {
    for (TxnSource s : values()) {
      if (s.name().equals(value)) {
        return true;
      }
    }
    return false;
  }

  public boolean isImport() {
    return this == BANK_IMPORT || this == UPI_IMPORT || this == GATEWAY_SETTLEMENT;
  }
}
