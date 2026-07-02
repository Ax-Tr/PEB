package com.paywithease.privacy.domain;

/**
 * Decides how an erasure request must be honoured for each data category, reconciling the data
 * principal's right to erasure with statutory retention duties. The overriding rule (and a platform
 * non-negotiable): financial and tax records are <b>never hard-deleted</b> — they are retained
 * under legal hold and their linked personal identifiers are anonymised instead.
 */
public final class RetentionPolicy {

  private RetentionPolicy() {}

  public enum Action {
    DELETE, // may be fully removed
    ANONYMIZE, // row/record kept but personal identifiers irreversibly removed
    RETAIN_LEGAL_HOLD // must be retained for a statutory period; PII anonymised, record kept
  }

  public record Outcome(Action action, int minRetentionYears, String reason) {}

  public static Outcome decide(DataCategory category) {
    return switch (category) {
      case MARKETING ->
          new Outcome(Action.DELETE, 0, "Consent-based marketing data may be fully deleted");
      case PROFILE_PII, CONTACT_PII ->
          new Outcome(
              Action.ANONYMIZE,
              0,
              "Profile/contact PII is anonymised where linked to retained records");
      case KYC_PII ->
          new Outcome(
              Action.RETAIN_LEGAL_HOLD,
              8,
              "KYC identifiers must be retained (PMLA/tax) then anonymised; kept under legal hold");
      case FINANCIAL_TXN ->
          new Outcome(
              Action.RETAIN_LEGAL_HOLD,
              8,
              "Financial records are never hard-deleted; retained ~8 years, linked PII anonymised");
      case TAX_RECORD ->
          new Outcome(
              Action.RETAIN_LEGAL_HOLD,
              8,
              "Tax records retained for the statutory period (Income-tax/GST)");
      case AUDIT_TRAIL ->
          new Outcome(
              Action.RETAIN_LEGAL_HOLD,
              8,
              "Audit trail is immutable and retained for accountability");
    };
  }
}
