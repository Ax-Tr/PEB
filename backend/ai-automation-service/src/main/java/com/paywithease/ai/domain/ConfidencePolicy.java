package com.paywithease.ai.domain;

/**
 * The governance gate for every AI output. Turns a (kind, confidence) pair into a decision:
 *
 * <ul>
 *   <li>{@link Decision#AUTO_APPLY} — only for auto-applicable, non-statutory kinds at or above the
 *       high-confidence threshold.
 *   <li>{@link Decision#NEEDS_REVIEW} — surfaced to a human to accept/reject.
 *   <li>{@link Decision#REJECT} — confidence too low to be actionable.
 * </ul>
 *
 * Hard rules that no threshold can override: statutory/filing kinds are never auto-applied, and
 * kinds that are not auto-applicable (e.g. OCR bank-detail extraction) always require review.
 */
public final class ConfidencePolicy {

  public enum Decision {
    AUTO_APPLY,
    NEEDS_REVIEW,
    REJECT
  }

  public static final ConfidencePolicy DEFAULT = new ConfidencePolicy(0.90, 0.50);

  private final double autoApplyThreshold;
  private final double reviewThreshold;

  public ConfidencePolicy(double autoApplyThreshold, double reviewThreshold) {
    if (reviewThreshold > autoApplyThreshold) {
      throw new IllegalArgumentException("reviewThreshold must be <= autoApplyThreshold");
    }
    this.autoApplyThreshold = autoApplyThreshold;
    this.reviewThreshold = reviewThreshold;
  }

  public Decision decide(SuggestionKind kind, double confidence) {
    double c = clamp(confidence);
    // Statutory or non-auto-applicable kinds can never be auto-applied.
    if (kind.isStatutory() || !kind.isAutoApplicable()) {
      return c >= reviewThreshold ? Decision.NEEDS_REVIEW : Decision.REJECT;
    }
    if (c >= autoApplyThreshold) {
      return Decision.AUTO_APPLY;
    }
    if (c >= reviewThreshold) {
      return Decision.NEEDS_REVIEW;
    }
    return Decision.REJECT;
  }

  private static double clamp(double c) {
    if (c < 0) {
      return 0;
    }
    return Math.min(c, 1.0);
  }
}
