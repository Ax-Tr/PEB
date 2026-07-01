package com.paywithease.common.security;

/**
 * Static holder that exposes the configured {@link AesGcmCipher} to JPA {@link
 * jakarta.persistence.AttributeConverter}s (which Hibernate instantiates outside the Spring
 * container). A {@code @Configuration} calls {@link #init(AesGcmCipher)} at startup; tests call it
 * with a test key. Kept intentionally tiny and explicit to avoid relying on Hibernate bean
 * injection.
 */
public final class FieldCrypto {

  private static volatile AesGcmCipher cipher;

  private FieldCrypto() {}

  public static void init(AesGcmCipher c) {
    cipher = c;
  }

  public static AesGcmCipher cipher() {
    AesGcmCipher c = cipher;
    if (c == null) {
      throw new IllegalStateException("FieldCrypto not initialized — no field-encryption key set");
    }
    return c;
  }
}
