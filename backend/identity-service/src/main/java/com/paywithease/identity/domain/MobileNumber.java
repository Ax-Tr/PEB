package com.paywithease.identity.domain;

import java.util.regex.Pattern;

/**
 * Indian mobile number value object: normalizes to a 10-digit form and validates the [6-9] prefix.
 */
public final class MobileNumber {

  private static final Pattern VALID = Pattern.compile("[6-9]\\d{9}");

  private final String value;

  private MobileNumber(String value) {
    this.value = value;
  }

  public static MobileNumber of(String raw) {
    String normalized = normalize(raw);
    if (!VALID.matcher(normalized).matches()) {
      throw new IllegalArgumentException("Invalid Indian mobile number");
    }
    return new MobileNumber(normalized);
  }

  public static boolean isValid(String raw) {
    try {
      of(raw);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** Strips spaces, hyphens, and a leading +91 / 91 / 0. */
  static String normalize(String raw) {
    if (raw == null) {
      return "";
    }
    String digits = raw.replaceAll("[\\s-]", "");
    if (digits.startsWith("+91")) {
      digits = digits.substring(3);
    } else if (digits.startsWith("91") && digits.length() == 12) {
      digits = digits.substring(2);
    } else if (digits.startsWith("0") && digits.length() == 11) {
      digits = digits.substring(1);
    }
    return digits;
  }

  public String value() {
    return value;
  }
}
