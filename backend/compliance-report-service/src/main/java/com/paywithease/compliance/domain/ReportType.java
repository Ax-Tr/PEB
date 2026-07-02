package com.paywithease.compliance.domain;

/** Kinds of compliance report this service prepares (preparation, not statutory filing). */
public enum ReportType {
  GSTR1_SUMMARY,
  GSTR3B_SUMMARY,
  SALES_REGISTER,
  PURCHASE_REGISTER,
  ITC_SUMMARY,
  TDS_SUMMARY,
  PAYROLL_COMPLIANCE,
  ITR_SUMMARY;

  public static boolean isValid(String value) {
    for (ReportType t : values()) {
      if (t.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
