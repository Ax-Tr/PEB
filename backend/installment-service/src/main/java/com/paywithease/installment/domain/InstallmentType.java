package com.paywithease.installment.domain;

/** Direction of the installment schedule. */
public enum InstallmentType {
  RECEIVABLE, // money the business will collect (customer EMIs)
  PAYABLE; // money the business will pay (vendor EMIs)

  public static boolean isValid(String value) {
    for (InstallmentType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
