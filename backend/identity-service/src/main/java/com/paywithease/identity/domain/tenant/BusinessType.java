package com.paywithease.identity.domain.tenant;

/** Legal constitution of an Indian MSME. */
public enum BusinessType {
  PROPRIETOR,
  PARTNERSHIP,
  LLP,
  PVT_LTD,
  PUBLIC_LTD,
  HUF,
  TRUST,
  SOCIETY,
  OTHER;

  public static boolean isValid(String value) {
    if (value == null) {
      return false;
    }
    for (BusinessType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
