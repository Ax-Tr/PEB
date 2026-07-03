package com.paywithease.ocr.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BankDetailExtractor {

  private static final Pattern IFSC = Pattern.compile("\\b[A-Z]{4}0[A-Z0-9]{6}\\b");
  private static final Pattern UPI = Pattern.compile("\\b[\\w.-]+@[\\w.-]+\\b");
  private static final Pattern LABELLED_ACCOUNT =
      Pattern.compile("(?i)(?:account|a/c|acct)[^0-9]{0,20}(\\d{9,18})");
  private static final Pattern ANY_ACCOUNT = Pattern.compile("\\b\\d{9,18}\\b");
  private static final Pattern LABELLED_NAME =
      Pattern.compile("(?i)(?:holder|name|beneficiary)\\s*[:\\-]\\s*([A-Z][A-Z .]{2,80})");
  private static final String[] COMMON_BANKS = {
    "HDFC BANK",
    "ICICI BANK",
    "STATE BANK OF INDIA",
    "SBI",
    "AXIS BANK",
    "KOTAK MAHINDRA BANK",
    "YES BANK",
    "BANK OF BARODA",
    "PUNJAB NATIONAL BANK",
    "INDUSIND BANK"
  };

  public BankDetailExtraction extract(String rawText) {
    String normalized = rawText == null ? "" : rawText.toUpperCase(Locale.ROOT);
    Map<String, ExtractedField> fields = new LinkedHashMap<>();

    putIfPresent(fields, "accountNumber", first(LABELLED_ACCOUNT, normalized, 1), "LABELLED", 0.95);
    if (!fields.containsKey("accountNumber")) {
      putIfPresent(fields, "accountNumber", first(ANY_ACCOUNT, normalized, 0), "UNLABELLED", 0.72);
    }
    putIfPresent(fields, "ifsc", first(IFSC, normalized, 0), "IFSC_PATTERN", 0.98);
    putIfPresent(fields, "upi", first(UPI, rawText == null ? "" : rawText, 0), "UPI_PATTERN", 0.85);
    putIfPresent(
        fields, "holderName", cleanName(first(LABELLED_NAME, normalized, 1)), "LABELLED", 0.82);
    putIfPresent(fields, "bankName", bankName(normalized), "BANK_DICTIONARY", 0.80);

    BigDecimal confidence =
        BigDecimal.valueOf(
                fields.values().stream()
                    .mapToDouble(ExtractedField::confidence)
                    .average()
                    .orElse(0))
            .setScale(4, RoundingMode.HALF_UP);
    return new BankDetailExtraction(fields, confidence);
  }

  private static void putIfPresent(
      Map<String, ExtractedField> fields,
      String key,
      String value,
      String source,
      double confidence) {
    if (value != null && !value.isBlank()) {
      fields.put(key, new ExtractedField(value.trim(), source, confidence));
    }
  }

  private static String first(Pattern pattern, String value, int group) {
    Matcher matcher = pattern.matcher(value);
    return matcher.find() ? matcher.group(group) : null;
  }

  private static String cleanName(String value) {
    if (value == null) {
      return null;
    }
    return value.replaceAll("\\s+", " ").trim();
  }

  private static String bankName(String normalized) {
    for (String bank : COMMON_BANKS) {
      if (normalized.contains(bank)) {
        return "SBI".equals(bank) ? "State Bank of India" : toTitleCase(bank);
      }
    }
    for (String line : normalized.split("\\R")) {
      if (line.contains("BANK")) {
        return toTitleCase(line.trim());
      }
    }
    return null;
  }

  private static String toTitleCase(String value) {
    StringBuilder out = new StringBuilder();
    for (String part : value.toLowerCase(Locale.ROOT).split(" ")) {
      if (!part.isBlank()) {
        out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
      }
    }
    return out.toString().trim();
  }

  public record ExtractedField(String value, String source, double confidence) {}

  public record BankDetailExtraction(Map<String, ExtractedField> fields, BigDecimal confidence) {}
}
