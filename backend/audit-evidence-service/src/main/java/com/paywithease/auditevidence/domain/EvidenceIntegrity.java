package com.paywithease.auditevidence.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Pure content-integrity helper for the evidence room. Evidence is stored with a SHA-256 hash so
 * its integrity can be independently re-verified at any time; if the recomputed hash differs from
 * the stored one, the artifact has been tampered with.
 */
public final class EvidenceIntegrity {

  private EvidenceIntegrity() {}

  public static String sha256Hex(byte[] content) {
    if (content == null) {
      throw new IllegalArgumentException("content must not be null");
    }
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
      return toHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public static String sha256Hex(String content) {
    return sha256Hex(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
  }

  /** Constant-time comparison of a stored hash against the hash of the supplied content. */
  public static boolean verify(String expectedHex, byte[] content) {
    if (expectedHex == null) {
      return false;
    }
    String actual = sha256Hex(content);
    return MessageDigest.isEqual(
        expectedHex.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }
}
