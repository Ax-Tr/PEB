package com.paywithease.common.security;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Pattern;

/**
 * Logback converter that masks PII/secrets in log messages (rule: no sensitive data in logs). Masks
 * Indian mobile numbers, PAN, GSTIN, IFSC, bank account numbers, emails, and UPI IDs. Registered as
 * conversion word {@code %maskedMsg} in logback config.
 */
public class PiiMaskingConverter extends MessageConverter {

  private static final Pattern MOBILE = Pattern.compile("\\b(?:\\+91)?[6-9]\\d{9}\\b");
  private static final Pattern PAN = Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b");
  private static final Pattern GSTIN =
      Pattern.compile("\\b\\d{2}[A-Z]{5}\\d{4}[A-Z]\\d[A-Z][A-Z0-9]\\b");
  private static final Pattern IFSC = Pattern.compile("\\b[A-Z]{4}0[A-Z0-9]{6}\\b");
  private static final Pattern ACCOUNT = Pattern.compile("\\b\\d{9,18}\\b");
  private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.-]+\\b");
  private static final Pattern UPI = Pattern.compile("\\b[\\w.-]{2,}@[a-zA-Z]{2,}\\b");

  @Override
  public String convert(ILoggingEvent event) {
    return mask(super.convert(event));
  }

  static String mask(String message) {
    if (message == null || message.isEmpty()) {
      return message;
    }
    String result = message;
    result = MOBILE.matcher(result).replaceAll(m -> keepLast(m.group(), 2));
    result = PAN.matcher(result).replaceAll("PAN_****");
    result = GSTIN.matcher(result).replaceAll("GSTIN_****");
    result = IFSC.matcher(result).replaceAll("IFSC_****");
    result = EMAIL.matcher(result).replaceAll(PiiMaskingConverter::maskEmail);
    result = UPI.matcher(result).replaceAll("****@upi");
    result = ACCOUNT.matcher(result).replaceAll(m -> keepLast(m.group(), 4));
    return result;
  }

  private static String keepLast(String value, int keep) {
    if (value.length() <= keep) {
      return "*".repeat(value.length());
    }
    return "*".repeat(value.length() - keep) + value.substring(value.length() - keep);
  }

  private static String maskEmail(java.util.regex.MatchResult m) {
    String email = m.group();
    int at = email.indexOf('@');
    if (at <= 1) {
      return "****" + email.substring(at);
    }
    return email.charAt(0) + "***" + email.substring(at);
  }
}
