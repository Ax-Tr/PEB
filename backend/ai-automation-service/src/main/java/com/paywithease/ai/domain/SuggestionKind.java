package com.paywithease.ai.domain;

/**
 * Kinds of AI suggestion, each tagged with two governance flags:
 *
 * <ul>
 *   <li>{@code autoApplicable} — may this kind ever be auto-applied at high confidence?
 *   <li>{@code statutory} — does this touch a statutory/filing action? Statutory actions are NEVER
 *       auto-applied and must always be performed by a human (product rule: no autonomous filing).
 * </ul>
 */
public enum SuggestionKind {
  TRANSACTION_CATEGORY(true, false),
  BANK_DETAIL_EXTRACTION(false, false), // OCR of bank details must always be user-reviewed
  CASHFLOW_FORECAST(false, false), // advisory only, never auto-applied
  ANOMALY_REVIEW(false, false),
  STATUTORY_FILING(false, true), // e.g. drafting a return — never autonomous
  GENERIC(false, false);

  private final boolean autoApplicable;
  private final boolean statutory;

  SuggestionKind(boolean autoApplicable, boolean statutory) {
    this.autoApplicable = autoApplicable;
    this.statutory = statutory;
  }

  public boolean isAutoApplicable() {
    return autoApplicable && !statutory;
  }

  public boolean isStatutory() {
    return statutory;
  }

  public static boolean isValid(String value) {
    for (SuggestionKind k : values()) {
      if (k.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
