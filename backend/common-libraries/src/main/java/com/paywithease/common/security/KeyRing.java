package com.paywithease.common.security;

import java.util.HashMap;
import java.util.Map;

/**
 * A versioned set of AES-GCM keys that makes key rotation safe and non-breaking. New data is always
 * encrypted with the current active key version; any value ever written can still be decrypted
 * because every prior key version is retained until all its ciphertext has been re-encrypted.
 *
 * <p>Ciphertext produced here is prefixed with its key version — {@code v{n}:base64(iv||ct||tag)} —
 * so decryption can dispatch to the correct key. Legacy values with no version prefix are decrypted
 * with the version-0 key, allowing a gradual migration from the single-key {@link AesGcmCipher}.
 *
 * <p>Rotation procedure: add a new key at {@code activeVersion+1} via {@link #withNewActiveKey},
 * deploy (new writes use it, old reads still work), then run a background re-encryption sweep, then
 * retire the old key version.
 */
public final class KeyRing {

  private static final String SEPARATOR = ":";

  private final int activeVersion;
  private final Map<Integer, AesGcmCipher> keysByVersion;

  private KeyRing(int activeVersion, Map<Integer, AesGcmCipher> keysByVersion) {
    if (!keysByVersion.containsKey(activeVersion)) {
      throw new IllegalArgumentException("No key for the active version " + activeVersion);
    }
    this.activeVersion = activeVersion;
    this.keysByVersion = Map.copyOf(keysByVersion);
  }

  public static KeyRing of(int activeVersion, Map<Integer, AesGcmCipher> keysByVersion) {
    return new KeyRing(activeVersion, keysByVersion);
  }

  /** Single-key ring at version 0 — the starting point before any rotation. */
  public static KeyRing single(AesGcmCipher key) {
    return new KeyRing(0, Map.of(0, key));
  }

  /** Returns a new ring with {@code newKey} added at {@code newVersion} and made active. */
  public KeyRing withNewActiveKey(int newVersion, AesGcmCipher newKey) {
    if (keysByVersion.containsKey(newVersion)) {
      throw new IllegalArgumentException("Version " + newVersion + " already exists in the ring");
    }
    Map<Integer, AesGcmCipher> next = new HashMap<>(keysByVersion);
    next.put(newVersion, newKey);
    return new KeyRing(newVersion, next);
  }

  public int activeVersion() {
    return activeVersion;
  }

  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    String ct = keysByVersion.get(activeVersion).encrypt(plaintext);
    return "v" + activeVersion + SEPARATOR + ct;
  }

  public String decrypt(String token) {
    if (token == null) {
      return null;
    }
    int version = 0;
    String ciphertext = token;
    if (token.startsWith("v")) {
      int sep = token.indexOf(SEPARATOR);
      if (sep > 1) {
        try {
          version = Integer.parseInt(token.substring(1, sep));
          ciphertext = token.substring(sep + 1);
        } catch (NumberFormatException ignored) {
          // Not a versioned token after all — treat the whole thing as legacy (version 0).
          version = 0;
          ciphertext = token;
        }
      }
    }
    AesGcmCipher cipher = keysByVersion.get(version);
    if (cipher == null) {
      throw new IllegalStateException("No key available for ciphertext version " + version);
    }
    return cipher.decrypt(ciphertext);
  }

  /**
   * True if the token was written with an older key and should be re-encrypted with the active key.
   */
  public boolean needsReEncryption(String token) {
    if (token == null) {
      return false;
    }
    return !token.startsWith("v" + activeVersion + SEPARATOR);
  }
}
