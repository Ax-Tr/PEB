package com.paywithease.ai.domain;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class VoiceIntentParser {

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
  private static final Pattern AMOUNT =
      Pattern.compile(
          "(?:rs\\.?|inr|₹)?\\s*(\\d+(?:,\\d{2,3})*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
  private static final Pattern PROMISE_COUNTERPARTY =
      Pattern.compile("(?i)\\b([A-Z][A-Za-z .]{1,60}?)\\s+(?:promised|promise|will pay|to pay)");
  private static final Pattern PAY_COUNTERPARTY =
      Pattern.compile("(?i)(?:from|by|for)\\s+([A-Z][A-Za-z .]{1,60})");

  private final Clock clock;

  public VoiceIntentParser(Clock clock) {
    this.clock = clock;
  }

  public ParsedVoiceIntent parse(String transcript) {
    String text = transcript == null ? "" : transcript.trim();
    String lower = text.toLowerCase(Locale.ROOT);
    if (lower.contains("installment") || lower.contains("emi")) {
      return missing(VoiceIntent.CREATE_INSTALLMENT, "counterpartyName", "amountMinor", "dueDate");
    }
    if (lower.contains("remind") || lower.contains("reminder")) {
      return missing(VoiceIntent.CREATE_REMINDER, "recipient", "dueDate");
    }
    if (lower.contains("expense") || lower.contains("spent") || lower.contains("paid for")) {
      return missing(VoiceIntent.CREATE_EXPENSE, "vendorName", "amountMinor", "expenseDate");
    }
    if (lower.contains("payout")) {
      return missing(VoiceIntent.CREATE_PAYOUT_REMINDER, "vendorName", "amountMinor", "dueDate");
    }
    if (lower.contains("note")) {
      return missing(VoiceIntent.ADD_CUSTOMER_NOTE, "customerName", "note");
    }
    if (lower.contains("promised") || lower.contains("will pay") || lower.contains("to pay")) {
      return parseCommitment(text, lower);
    }
    return missing(VoiceIntent.UNKNOWN, "intent");
  }

  private ParsedVoiceIntent parseCommitment(String text, String lower) {
    Map<String, Object> fields = new LinkedHashMap<>();
    String counterparty = first(PROMISE_COUNTERPARTY, text, 1);
    if (counterparty == null) {
      counterparty = first(PAY_COUNTERPARTY, text, 1);
    }
    if (counterparty != null) {
      fields.put("counterpartyName", cleanName(counterparty));
      fields.put("counterpartyType", "CUSTOMER");
    }
    Long amountMinor = amountMinor(text);
    if (amountMinor != null) {
      fields.put("amountMinor", amountMinor);
    }
    LocalDate dueDate = dueDate(lower);
    if (dueDate != null) {
      fields.put("dueDate", dueDate.toString());
    }
    fields.put("sourceType", "VOICE");
    fields.put("description", text);
    List<String> missing =
        List.of("counterpartyName", "amountMinor", "dueDate").stream()
            .filter(k -> !fields.containsKey(k))
            .toList();
    double confidence = Math.max(0.25, 0.95 - (missing.size() * 0.20));
    return new ParsedVoiceIntent(VoiceIntent.CREATE_COMMITMENT, fields, missing, confidence);
  }

  private static ParsedVoiceIntent missing(VoiceIntent intent, String... fields) {
    return new ParsedVoiceIntent(intent, Map.of(), List.of(fields), 0.35);
  }

  private Long amountMinor(String text) {
    Matcher matcher = AMOUNT.matcher(text.replace(",", ""));
    while (matcher.find()) {
      String value = matcher.group(1);
      if (value.length() >= 2) {
        return Math.round(Double.parseDouble(value) * 100);
      }
    }
    return null;
  }

  private LocalDate dueDate(String lower) {
    LocalDate today = LocalDate.now(clock.withZone(IST));
    if (lower.contains("today")) {
      return today;
    }
    if (lower.contains("tomorrow")) {
      return today.plusDays(1);
    }
    if (lower.contains("friday")) {
      return nextOrSame(today, DayOfWeek.FRIDAY);
    }
    if (lower.contains("monday")) {
      return nextOrSame(today, DayOfWeek.MONDAY);
    }
    Matcher matcher = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b").matcher(lower);
    return matcher.find() ? LocalDate.parse(matcher.group(1)) : null;
  }

  private static LocalDate nextOrSame(LocalDate today, DayOfWeek day) {
    return today.with(TemporalAdjusters.nextOrSame(day));
  }

  private static String first(Pattern pattern, String value, int group) {
    Matcher matcher = pattern.matcher(value);
    return matcher.find() ? matcher.group(group) : null;
  }

  private static String cleanName(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }
}
