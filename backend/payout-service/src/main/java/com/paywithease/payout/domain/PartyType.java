package com.paywithease.payout.domain;

/** Who is being paid. */
public enum PartyType {
  VENDOR,
  EMPLOYEE;

  public static boolean isValid(String value) {
    for (PartyType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
