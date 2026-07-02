package com.paywithease.ingestion.domain;

/** Money direction of a transaction. */
public enum Direction {
  CREDIT, // money in
  DEBIT; // money out

  public static boolean isValid(String value) {
    for (Direction d : values()) {
      if (d.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
