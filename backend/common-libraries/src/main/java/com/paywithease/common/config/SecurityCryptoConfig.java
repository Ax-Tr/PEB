package com.paywithease.common.config;

import com.paywithease.common.security.AesGcmCipher;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.security.FieldCrypto;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires field-level encryption + blind-index keys for every service that scans {@code
 * com.paywithease.common}. In production these come from KMS/Vault (per-tenant strategy,
 * ADR/decisions); the built-in defaults are development-only and log a loud warning.
 */
@Configuration
public class SecurityCryptoConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityCryptoConfig.class);

  // 32 zero bytes / 16 zero bytes — DEV ONLY. Override in every real environment.
  private static final String DEV_FIELD_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String DEV_BLIND_KEY = "AAAAAAAAAAAAAAAAAAAAAAA=";

  @Value("${peb.security.field-key:" + DEV_FIELD_KEY + "}")
  private String fieldKeyBase64;

  @Value("${peb.security.blind-index-key:" + DEV_BLIND_KEY + "}")
  private String blindKeyBase64;

  @Bean
  public AesGcmCipher fieldCipher() {
    if (DEV_FIELD_KEY.equals(fieldKeyBase64)) {
      log.warn(
          "SECURITY: using the built-in DEV field-encryption key. "
              + "Set peb.security.field-key from KMS/Vault before any non-local deployment.");
    }
    AesGcmCipher cipher = AesGcmCipher.fromBase64Key(fieldKeyBase64);
    FieldCrypto.init(cipher);
    return cipher;
  }

  @Bean
  public BlindIndex blindIndex() {
    if (DEV_BLIND_KEY.equals(blindKeyBase64)) {
      log.warn("SECURITY: using the built-in DEV blind-index key. Override before deployment.");
    }
    return new BlindIndex(Base64.getDecoder().decode(blindKeyBase64));
  }
}
