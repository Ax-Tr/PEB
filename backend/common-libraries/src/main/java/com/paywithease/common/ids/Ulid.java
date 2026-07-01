package com.paywithease.common.ids;

import com.github.f4b6a3.ulid.UlidCreator;
import java.util.regex.Pattern;

/**
 * ULID helper. IDs are lexicographically sortable, 26-char Crockford base32 strings stored as
 * {@code char(26)} (ADR/decisions). Generated in the domain layer, never by the database.
 */
public final class Ulid {

  private static final Pattern ULID_PATTERN = Pattern.compile("[0-7][0-9A-HJKMNP-TV-Z]{25}");

  private Ulid() {}

  /** Generate a new monotonic ULID string. */
  public static String newId() {
    return UlidCreator.getMonotonicUlid().toString();
  }

  public static boolean isValid(String value) {
    return value != null && ULID_PATTERN.matcher(value).matches();
  }

  public static String requireValid(String value) {
    if (!isValid(value)) {
      throw new IllegalArgumentException("Not a valid ULID: " + value);
    }
    return value;
  }
}
