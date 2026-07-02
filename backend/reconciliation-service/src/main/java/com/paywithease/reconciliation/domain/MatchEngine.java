package com.paywithease.reconciliation.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Weighted reconciliation matcher (pure). Scores an external (bank) item against internal
 * candidates on the signals from the AI/algorithms blueprint — amount, reference
 * (UTR/UPI/gateway/invoice no.), date proximity, counterparty and narration similarity — and
 * classifies the best pair as AUTO, SUGGESTED, or EXCEPTION. Amount is decisive: a mismatch (or a
 * direction mismatch) can never auto-match. Weights sum to 1.0.
 */
public final class MatchEngine {

  private MatchEngine() {}

  public static final double AUTO_THRESHOLD = 0.90;
  public static final double SUGGEST_THRESHOLD = 0.60;

  private static final double W_AMOUNT = 0.45;
  private static final double W_REFERENCE = 0.35;
  private static final double W_DATE = 0.10;
  private static final double W_COUNTERPARTY = 0.05;
  private static final double W_NARRATION = 0.05;

  public enum Decision {
    AUTO,
    SUGGESTED,
    EXCEPTION
  }

  /** A reconcilable item (either side). */
  public record Item(
      String id,
      String direction,
      long amountMinor,
      LocalDate date,
      String reference,
      String counterparty,
      String narration) {}

  public record Result(String candidateId, double score, Decision decision) {}

  /** Score of one external item against one candidate (0..1). Gated by direction + amount. */
  public static double score(Item external, Item candidate) {
    if (external.direction() != null
        && candidate.direction() != null
        && !external.direction().equals(candidate.direction())) {
      return 0.0; // opposite money direction can never be the same transaction
    }

    double total = 0.0;
    if (external.amountMinor() == candidate.amountMinor()) {
      total += W_AMOUNT;
    }
    total += referenceScore(external.reference(), candidate.reference());
    total += dateScore(external.date(), candidate.date());
    total +=
        W_COUNTERPARTY
            * StringSimilarity.jaccard(external.counterparty(), candidate.counterparty());
    total += narrationScore(external, candidate);
    return Math.min(1.0, total);
  }

  /** Best candidate for an external item, with its decision band. */
  public static Result match(Item external, List<Item> candidates) {
    String bestId = null;
    double best = 0.0;
    for (Item c : candidates) {
      double s = score(external, c);
      if (s > best) {
        best = s;
        bestId = c.id();
      }
    }
    Decision decision;
    if (bestId != null && best >= AUTO_THRESHOLD) {
      decision = Decision.AUTO;
    } else if (bestId != null && best >= SUGGEST_THRESHOLD) {
      decision = Decision.SUGGESTED;
    } else {
      decision = Decision.EXCEPTION; // no confident pairing → needs review
    }
    return new Result(decision == Decision.EXCEPTION ? null : bestId, best, decision);
  }

  private static double referenceScore(String a, String b) {
    String na = normalize(a);
    String nb = normalize(b);
    if (na.isEmpty() || nb.isEmpty()) {
      return 0.0;
    }
    if (na.equals(nb)) {
      return W_REFERENCE;
    }
    if (na.contains(nb) || nb.contains(na)) {
      return W_REFERENCE * 0.6; // partial reference overlap
    }
    return 0.0;
  }

  private static double dateScore(LocalDate a, LocalDate b) {
    if (a == null || b == null) {
      return 0.0;
    }
    long days = Math.abs(ChronoUnit.DAYS.between(a, b));
    if (days == 0) {
      return W_DATE;
    }
    if (days <= 2) {
      return W_DATE * 0.7;
    }
    if (days <= 5) {
      return W_DATE * 0.4;
    }
    return 0.0;
  }

  private static double narrationScore(Item external, Item candidate) {
    String ref = normalize(candidate.reference());
    if (!ref.isEmpty() && normalize(external.narration()).contains(ref)) {
      return W_NARRATION; // the bank narration mentions our reference
    }
    return W_NARRATION * StringSimilarity.jaccard(external.narration(), candidate.narration());
  }

  private static String normalize(String s) {
    return s == null ? "" : s.trim().toUpperCase().replaceAll("\\s+", "");
  }
}
