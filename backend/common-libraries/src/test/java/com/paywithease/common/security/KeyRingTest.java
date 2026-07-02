package com.paywithease.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class KeyRingTest {

  private static AesGcmCipher key(byte fill) {
    byte[] bytes = new byte[32];
    java.util.Arrays.fill(bytes, fill);
    return new AesGcmCipher(bytes);
  }

  @Test
  void encryptsWithActiveVersionAndRoundTrips() {
    KeyRing ring = KeyRing.single(key((byte) 1));
    String token = ring.encrypt("PAN:ABCDE1234F");
    assertThat(token).startsWith("v0:");
    assertThat(ring.decrypt(token)).isEqualTo("PAN:ABCDE1234F");
  }

  @Test
  void afterRotationNewWritesUseNewKeyButOldCiphertextStillDecrypts() {
    KeyRing v0 = KeyRing.single(key((byte) 1));
    String oldToken = v0.encrypt("secret");

    KeyRing v1 = v0.withNewActiveKey(1, key((byte) 2));
    assertThat(v1.activeVersion()).isEqualTo(1);

    // Old ciphertext (v0) is still readable through the rotated ring...
    assertThat(v1.decrypt(oldToken)).isEqualTo("secret");
    // ...and new writes use v1.
    String newToken = v1.encrypt("secret");
    assertThat(newToken).startsWith("v1:");
    assertThat(v1.decrypt(newToken)).isEqualTo("secret");
  }

  @Test
  void needsReEncryptionFlagsStaleVersions() {
    KeyRing v0 = KeyRing.single(key((byte) 1));
    String oldToken = v0.encrypt("x");
    KeyRing v1 = v0.withNewActiveKey(1, key((byte) 2));
    assertThat(v1.needsReEncryption(oldToken)).isTrue();
    assertThat(v1.needsReEncryption(v1.encrypt("x"))).isFalse();
  }

  @Test
  void legacyUnversionedTokenDecryptsWithVersionZero() {
    AesGcmCipher legacy = key((byte) 7);
    String legacyToken = legacy.encrypt("old-value"); // no version prefix
    KeyRing ring = KeyRing.of(0, Map.of(0, legacy));
    assertThat(ring.decrypt(legacyToken)).isEqualTo("old-value");
  }

  @Test
  void unknownVersionFailsLoudly() {
    KeyRing ring = KeyRing.single(key((byte) 1));
    assertThatThrownBy(() -> ring.decrypt("v9:AAAA")).isInstanceOf(IllegalStateException.class);
  }
}
