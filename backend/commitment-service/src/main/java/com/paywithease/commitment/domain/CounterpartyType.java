package com.paywithease.commitment.domain;

public enum CounterpartyType {
  CUSTOMER,
  VENDOR,
  EMPLOYEE,
  OTHER;

  public static boolean isValid(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    for (CounterpartyType type : values()) {
      if (type.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
