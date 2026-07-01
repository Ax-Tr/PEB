package com.paywithease.common.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM authenticated encryption for field-level protection of sensitive data (bank account,
 * IFSC, UPI, PAN, GSTIN, mobile — see specs/data-dictionary-conventions.md). Output is {@code
 * base64(iv || ciphertext || tag)}. The key is 32 bytes; in production it is KMS-backed and
 * per-tenant, injected from the secret manager — never hard-coded.
 */
public final class AesGcmCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12; // 96-bit nonce, recommended for GCM
  private static final int TAG_LENGTH_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public AesGcmCipher(byte[] keyBytes) {
    if (keyBytes == null || keyBytes.length != 32) {
      throw new IllegalArgumentException("AES-256 key must be exactly 32 bytes");
    }
    this.key = new SecretKeySpec(keyBytes, "AES");
  }

  public static AesGcmCipher fromBase64Key(String base64Key) {
    return new AesGcmCipher(Base64.getDecoder().decode(base64Key));
  }

  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] out = new byte[iv.length + ct.length];
      System.arraycopy(iv, 0, out, 0, iv.length);
      System.arraycopy(ct, 0, out, iv.length, ct.length);
      return Base64.getEncoder().encodeToString(out);
    } catch (Exception e) {
      throw new IllegalStateException("Field encryption failed", e);
    }
  }

  public String decrypt(String encoded) {
    if (encoded == null) {
      return null;
    }
    try {
      byte[] in = Base64.getDecoder().decode(encoded);
      byte[] iv = new byte[IV_LENGTH];
      System.arraycopy(in, 0, iv, 0, IV_LENGTH);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] pt = cipher.doFinal(in, IV_LENGTH, in.length - IV_LENGTH);
      return new String(pt, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Field decryption failed", e);
    }
  }
}
