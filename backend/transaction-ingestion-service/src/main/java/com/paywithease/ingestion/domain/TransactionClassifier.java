package com.paywithease.ingestion.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic, rule-based transaction classifier (a placeholder for the AI classifier in Sprint
 * 15). Matches bank-narration keywords to a category with a confidence score. Low-confidence
 * results must be user-reviewed before they are trusted — the caller (and AI later) enforce that.
 */
public final class TransactionClassifier {

  private TransactionClassifier() {}

  public record Suggestion(String category, BigDecimal confidence) {}

  private record Rule(String[] keywords, String category, String confidence) {}

  // Ordered: first match wins. Keywords are matched case-insensitively against the narration.
  private static final List<Rule> RULES =
      List.of(
          new Rule(new String[] {"SALARY", "SAL "}, "SALARY", "0.90"),
          new Rule(new String[] {"RENT"}, "RENT", "0.90"),
          new Rule(new String[] {"GST", "CGST", "SGST", "IGST", "TDS", "TAX"}, "GST_TAX", "0.90"),
          new Rule(new String[] {"EMI", "LOAN"}, "LOAN_EMI", "0.90"),
          new Rule(new String[] {"ATM", "CASH WD", "WITHDRAWAL"}, "CASH_WITHDRAWAL", "0.85"),
          new Rule(new String[] {"CHRG", "CHARGE", "FEE"}, "BANK_CHARGES", "0.85"),
          new Rule(
              new String[] {"ELECTRIC", "POWER", "WATER", "UTILITY", "RECHARGE"},
              "UTILITIES",
              "0.80"),
          new Rule(new String[] {"NEFT", "IMPS", "UPI", "TRANSFER", "RTGS"}, "TRANSFER", "0.50"));

  public static Suggestion classify(String narration, Direction direction) {
    String text = narration == null ? "" : narration.toUpperCase();
    for (Rule rule : RULES) {
      for (String kw : rule.keywords()) {
        if (text.contains(kw)) {
          return new Suggestion(rule.category(), new BigDecimal(rule.confidence()));
        }
      }
    }
    // No keyword: credits are likely sales receipts; debits are unknown until reviewed.
    if (direction == Direction.CREDIT) {
      return new Suggestion("SALES", new BigDecimal("0.50"));
    }
    return new Suggestion("UNKNOWN", new BigDecimal("0.30"));
  }
}
