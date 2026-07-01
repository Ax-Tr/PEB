package com.paywithease.payment.application;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Verifies inbound payment-webhook signatures. Each provider posts an HMAC-SHA-256 of the raw
 * request body computed with a shared secret (the Razorpay/Cashfree model). The signature is
 * verified <b>before</b> the body is parsed, in constant time, and an optional timestamp is checked
 * against a freshness window to blunt replay. Secrets come from config/Vault, never code.
 */
@Component
@ConfigurationProperties(prefix = "peb.payments")
public class GatewaySignatureVerifier {

  private static final Logger log = LoggerFactory.getLogger(GatewaySignatureVerifier.class);

  /** provider -> shared webhook secret. Bound from {@code peb.payments.webhook-secrets.*}. */
  private Map<String, String> webhookSecrets = Map.of();

  private final Clock clock;
  private final Duration replayWindow = Duration.ofMinutes(5);

  public GatewaySignatureVerifier(Clock clock) {
    this.clock = clock;
  }

  public void setWebhookSecrets(Map<String, String> webhookSecrets) {
    this.webhookSecrets = webhookSecrets;
  }

  @PostConstruct
  void warnIfUnconfigured() {
    if (webhookSecrets.isEmpty()) {
      log.warn(
          "SECURITY: no payment webhook secrets configured (peb.payments.webhook-secrets.*). "
              + "All inbound webhooks will fail signature verification until secrets are set.");
    }
  }

  /**
   * Returns true only if a secret exists for the provider and the HMAC matches in constant time.
   */
  public boolean verify(String provider, String rawBody, String signatureHeader) {
    String secret = webhookSecrets.get(provider);
    if (secret == null || signatureHeader == null || rawBody == null) {
      return false;
    }
    String expected = hmacSha256Hex(secret, rawBody);
    return constantTimeEquals(expected, signatureHeader.trim());
  }

  /** Optional replay defence when the provider sends a unix-seconds timestamp header. */
  public boolean isFresh(Long timestampEpochSeconds) {
    if (timestampEpochSeconds == null) {
      return true; // provider does not send a timestamp; rely on event-id idempotency instead
    }
    long now = clock.instant().getEpochSecond();
    return Math.abs(now - timestampEpochSeconds) <= replayWindow.toSeconds();
  }

  private static String hmacSha256Hex(String secret, String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Webhook signature computation failed", e);
    }
  }

  private static boolean constantTimeEquals(String a, String b) {
    return java.security.MessageDigest.isEqual(
        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}
