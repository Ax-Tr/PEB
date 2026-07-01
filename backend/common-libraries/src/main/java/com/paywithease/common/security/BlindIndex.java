package com.paywithease.common.security;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Deterministic keyed hash (HMAC-SHA-256) used as a <b>blind index</b>: lets a service do an
 * equality lookup (e.g. find a user by mobile) against an encrypted column without ever storing or
 * querying the plaintext. Same input + key → same hex digest, so it is safe to index and unique-
 * constrain, while revealing nothing about the value.
 */
public final class BlindIndex {

  private final byte[] key;

  public BlindIndex(byte[] key) {
    if (key == null || key.length < 16) {
      throw new IllegalArgumentException("Blind-index key must be at least 16 bytes");
    }
    this.key = key.clone();
  }

  public String hash(String value) {
    if (value == null) {
      return null;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      byte[] digest = mac.doFinal(value.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("Blind index computation failed", e);
    }
  }
}
