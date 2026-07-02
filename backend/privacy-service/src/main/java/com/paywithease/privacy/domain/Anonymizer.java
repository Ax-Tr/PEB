package com.paywithease.privacy.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Pure, irreversible anonymisation of personal identifiers. Values are replaced with
 * format-preserving masks (so downstream schemas/validation still hold) or with a salted one-way
 * token where a stable pseudonym is needed. There is no un-anonymise path — that is the point.
 */
public final class Anonymizer {

  private Anonymizer() {}

  public static final String REDACTED = "REDACTED";

  public static String name(String value) {
    return value == null || value.isBlank() ? value : REDACTED;
  }

  /** Keep the domain (useful for analytics) but irreversibly remove the local part. */
  public static String email(String value) {
    if (value == null || !value.contains("@")) {
      return value == null ? null : REDACTED;
    }
    String domain = value.substring(value.indexOf('@') + 1);
    return "redacted@" + domain;
  }

  /** Mask an Indian mobile number, keeping length/shape. */
  public static String phone(String value) {
    if (value == null) {
      return null;
    }
    return value.replaceAll("\\d", "X");
  }

  /** Mask a PAN (ABCDE1234F) preserving its 5-4-1 shape. */
  public static String pan(String value) {
    return value == null || value.isBlank() ? value : "XXXXX0000X";
  }

  /**
   * A stable, salted pseudonym for when references must be preserved without revealing the value.
   */
  public static String pseudonym(String value, String salt) {
    if (value == null) {
      return null;
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update((salt == null ? "" : salt).getBytes(StandardCharsets.UTF_8));
      byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder("anon_");
      for (int i = 0; i < 8; i++) {
        sb.append(Character.forDigit((digest[i] >> 4) & 0xF, 16));
        sb.append(Character.forDigit(digest[i] & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
