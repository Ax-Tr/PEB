package com.paywithease.privacy.domain;

/** Kinds of Data Subject Request under the DPDP Act. */
public enum DsrType {
  ACCESS,
  CORRECTION,
  ERASURE,
  PORTABILITY,
  GRIEVANCE;

  public static boolean isValid(String value) {
    for (DsrType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
