package com.paywithease.identity.domain.tenant;

import java.util.regex.Pattern;

/** Udyam registration number: {@code UDYAM-XX-00-0000000} (state letters, 2 + 7 digits). */
public final class Udyam {

  private static final Pattern FORMAT = Pattern.compile("UDYAM-[A-Z]{2}-\\d{2}-\\d{7}");

  private final String value;

  private Udyam(String value) {
    this.value = value;
  }

  public static Udyam of(String raw) {
    if (!isValid(raw)) {
      throw new IllegalArgumentException("Invalid Udyam registration number");
    }
    return new Udyam(raw.trim().toUpperCase());
  }

  public static boolean isValid(String raw) {
    return raw != null && FORMAT.matcher(raw.trim().toUpperCase()).matches();
  }

  public String value() {
    return value;
  }
}
