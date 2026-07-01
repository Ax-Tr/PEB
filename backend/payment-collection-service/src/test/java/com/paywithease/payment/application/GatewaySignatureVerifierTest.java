package com.paywithease.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewaySignatureVerifierTest {

  private static final String SECRET = "top-secret";
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
  private GatewaySignatureVerifier verifier;

  @BeforeEach
  void setUp() {
    verifier = new GatewaySignatureVerifier(clock);
    verifier.setWebhookSecrets(Map.of("UPI", SECRET));
  }

  @Test
  void verifiesCorrectSignature() {
    String body = "{\"eventId\":\"e1\"}";
    assertThat(verifier.verify("UPI", body, hmac(SECRET, body))).isTrue();
  }

  @Test
  void rejectsWrongSignature() {
    assertThat(verifier.verify("UPI", "{\"a\":1}", "deadbeef")).isFalse();
  }

  @Test
  void rejectsUnknownProviderOrNulls() {
    assertThat(verifier.verify("stripe", "{}", hmac(SECRET, "{}"))).isFalse();
    assertThat(verifier.verify("UPI", "{}", null)).isFalse();
    assertThat(verifier.verify("UPI", null, "x")).isFalse();
  }

  @Test
  void freshnessWindow() {
    long now = clock.instant().getEpochSecond();
    assertThat(verifier.isFresh(now)).isTrue();
    assertThat(verifier.isFresh(now - 60)).isTrue();
    assertThat(verifier.isFresh(now - 6000)).isFalse(); // beyond 5 min
    assertThat(verifier.isFresh(null)).isTrue(); // no timestamp -> rely on event-id idempotency
  }

  private static String hmac(String secret, String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
