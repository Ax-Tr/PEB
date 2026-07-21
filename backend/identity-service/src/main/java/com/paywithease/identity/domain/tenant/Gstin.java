package com.paywithease.identity.domain.tenant;

import java.util.regex.Pattern;

/**
 * GSTIN value object with format + check-digit validation. Structure: 2-digit state code, 10-char
 * PAN, 1 entity code, fixed 'Z', 1 checksum char.
 */
public final class Gstin {

  private static final String CODEPOINTS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int BASE = CODEPOINTS.length(); // 36
  private static final Pattern FORMAT =
      Pattern.compile("\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]");

  private final String value;

  private Gstin(String value) {
    this.value = value;
  }

  public static Gstin of(String raw) {
    if (!isValid(raw)) {
      throw new IllegalArgumentException("Invalid GSTIN");
    }
    return new Gstin(raw.trim().toUpperCase());
  }

  public static boolean isValid(String raw) {
    if (raw == null) {
      return false;
    }
    String g = raw.trim().toUpperCase();
    if (!FORMAT.matcher(g).matches()) {
      return false;
    }
    return g.charAt(14) == checkDigit(g.substring(0, 14));
  }

  /** Computes the 15th (check) character from the first 14 GSTIN characters. */
  public static char checkDigit(String first14) {
    int factor = 2;
    int sum = 0;
    for (int i = first14.length() - 1; i >= 0; i--) {
      int codePoint = CODEPOINTS.indexOf(first14.charAt(i));
      int digit = factor * codePoint;
      digit = (digit / BASE) + (digit % BASE);
      sum += digit;
      factor = (factor == 2) ? 1 : 2;
    }
    int checkCodePoint = (BASE - (sum % BASE)) % BASE;
    return CODEPOINTS.charAt(checkCodePoint);
  }

  public String value() {
    return value;
  }

  public String stateCode() {
    return value.substring(0, 2);
  }

  public String pan() {
    return value.substring(2, 12);
  }
}
