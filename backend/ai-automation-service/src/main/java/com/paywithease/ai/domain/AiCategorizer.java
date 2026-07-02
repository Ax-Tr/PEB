package com.paywithease.ai.domain;

import java.util.List;
import java.util.Locale;

/**
 * A deterministic, explainable transaction categoriser. It stands in for a learned model behind a
 * stable interface: given a narration it returns a category and a calibrated confidence. Being
 * rule-based makes it auditable and reproducible; a real model can replace it without changing the
 * governance around it.
 */
public final class AiCategorizer {

  private AiCategorizer() {}

  public record Category(String category, double confidence, String rationale) {}

  private record Rule(String category, double confidence, List<String> keywords) {}

  // Ordered: the first matching rule wins.
  private static final List<Rule> RULES =
      List.of(
          new Rule("SALARY", 0.95, List.of("salary", "payroll", "wages", "stipend")),
          new Rule("RENT", 0.93, List.of("rent", "lease")),
          new Rule(
              "UTILITIES",
              0.9,
              List.of(
                  "electricity", "power bill", "water bill", "gas bill", "broadband", "internet")),
          new Rule("TELECOM", 0.9, List.of("airtel", "jio", "vodafone", "vi ", "bsnl", "recharge")),
          new Rule("GST_PAYMENT", 0.92, List.of("gst", "gstn", "cgst", "sgst", "igst")),
          new Rule("TDS_PAYMENT", 0.92, List.of("tds", "tcs", "income tax", "advance tax")),
          new Rule(
              "BANK_CHARGES",
              0.88,
              List.of(
                  "bank charge", "service charge", "processing fee", "neft charge", "imps charge")),
          new Rule("FUEL", 0.87, List.of("petrol", "diesel", "fuel", "hpcl", "iocl", "bpcl")),
          new Rule(
              "TRAVEL", 0.85, List.of("uber", "ola", "irctc", "flight", "hotel", "makemytrip")),
          new Rule(
              "SUPPLIES", 0.8, List.of("stationery", "supplies", "amazon", "flipkart", "purchase")),
          new Rule(
              "PROFESSIONAL_FEES",
              0.82,
              List.of("consulting", "professional", "audit fee", "legal", "ca fee")));

  private static final Category UNCLASSIFIED =
      new Category("UNCATEGORISED", 0.2, "No rule matched");

  public static Category classify(String narration) {
    if (narration == null || narration.isBlank()) {
      return new Category("UNCATEGORISED", 0.0, "Empty narration");
    }
    String text = narration.toLowerCase(Locale.ROOT);
    for (Rule rule : RULES) {
      for (String kw : rule.keywords()) {
        if (text.contains(kw)) {
          return new Category(
              rule.category(), rule.confidence(), "Matched keyword: '" + kw.trim() + "'");
        }
      }
    }
    return UNCLASSIFIED;
  }
}
