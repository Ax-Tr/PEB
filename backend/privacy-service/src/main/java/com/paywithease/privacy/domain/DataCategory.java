package com.paywithease.privacy.domain;

/** Categories of personal/business data a data principal may have across the platform. */
public enum DataCategory {
  PROFILE_PII,
  CONTACT_PII,
  KYC_PII, // PAN / Aadhaar / bank details
  FINANCIAL_TXN, // invoices, payments, ledger entries
  TAX_RECORD, // GST/TDS/ITR working papers
  MARKETING, // consent-based marketing data
  AUDIT_TRAIL // immutable audit/evidence
}
