package com.paywithease.product.domain;

/** Whether a catalog line is a physical GOOD (HSN) or a SERVICE (SAC). */
public enum ItemType {
  GOOD,
  SERVICE;

  public static boolean isValid(String value) {
    if (value == null) {
      return false;
    }
    for (ItemType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
