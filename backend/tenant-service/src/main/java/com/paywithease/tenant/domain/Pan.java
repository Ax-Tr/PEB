package com.paywithease.tenant.domain;

import java.util.regex.Pattern;

/** PAN value object: 5 letters + 4 digits + 1 letter (e.g. ABCDE1234F). Format validation only. */
public final class Pan {

  private static final Pattern FORMAT = Pattern.compile("[A-Z]{5}\\d{4}[A-Z]");

  private final String value;

  private Pan(String value) {
    this.value = value;
  }

  public static Pan of(String raw) {
    if (!isValid(raw)) {
      throw new IllegalArgumentException("Invalid PAN");
    }
    return new Pan(raw.trim().toUpperCase());
  }

  public static boolean isValid(String raw) {
    return raw != null && FORMAT.matcher(raw.trim().toUpperCase()).matches();
  }

  public String value() {
    return value;
  }

  /** The 4th character encodes the holder type (P=individual, C=company, F=firm, ...). */
  public char holderType() {
    return value.charAt(3);
  }
}
