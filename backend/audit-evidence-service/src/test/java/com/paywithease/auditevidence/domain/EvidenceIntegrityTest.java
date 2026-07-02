package com.paywithease.auditevidence.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class EvidenceIntegrityTest {

  @Test
  void sha256IsDeterministicAndKnownLength() {
    String h1 = EvidenceIntegrity.sha256Hex("hello");
    String h2 = EvidenceIntegrity.sha256Hex("hello");
    assertThat(h1).isEqualTo(h2).hasSize(64);
    // Known SHA-256 of "hello"
    assertThat(h1).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
  }

  @Test
  void verifyDetectsMatchAndTamper() {
    byte[] content = "invoice-pdf-bytes".getBytes(StandardCharsets.UTF_8);
    String hash = EvidenceIntegrity.sha256Hex(content);
    assertThat(EvidenceIntegrity.verify(hash, content)).isTrue();
    assertThat(EvidenceIntegrity.verify(hash, "tampered".getBytes(StandardCharsets.UTF_8)))
        .isFalse();
  }

  @Test
  void verifyFalseWhenExpectedNull() {
    assertThat(EvidenceIntegrity.verify(null, new byte[] {1, 2, 3})).isFalse();
  }
}
