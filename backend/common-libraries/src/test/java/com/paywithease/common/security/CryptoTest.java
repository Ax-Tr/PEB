package com.paywithease.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class CryptoTest {

  private static final byte[] KEY32 = new byte[32];
  private static final byte[] KEY16 = new byte[16];

  @Test
  void aesGcmRoundTrips() {
    AesGcmCipher cipher = new AesGcmCipher(KEY32);
    String enc = cipher.encrypt("9876543210");
    assertThat(enc).isNotNull().isNotEqualTo("9876543210");
    assertThat(cipher.decrypt(enc)).isEqualTo("9876543210");
  }

  @Test
  void aesGcmProducesDifferentCiphertextEachTime() {
    AesGcmCipher cipher = new AesGcmCipher(KEY32);
    assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same")); // random IV
  }

  @Test
  void aesGcmNullPassthroughAndKeyValidation() {
    AesGcmCipher cipher = new AesGcmCipher(KEY32);
    assertThat(cipher.encrypt(null)).isNull();
    assertThat(cipher.decrypt(null)).isNull();
    assertThatThrownBy(() -> new AesGcmCipher(KEY16)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void blindIndexIsDeterministicAndCaseInsensitive() {
    BlindIndex index = new BlindIndex(KEY32);
    String h1 = index.hash("9876543210");
    String h2 = index.hash("9876543210");
    assertThat(h1).isEqualTo(h2).hasSize(64);
    assertThat(index.hash("ABC@Upi")).isEqualTo(index.hash("abc@upi"));
    assertThat(index.hash("9876543210")).isNotEqualTo(index.hash("9876543211"));
  }

  @Test
  void fieldCryptoConverterUsesInitializedCipher() {
    FieldCrypto.init(AesGcmCipher.fromBase64Key(Base64.getEncoder().encodeToString(KEY32)));
    EncryptedStringConverter converter = new EncryptedStringConverter();
    String db = converter.convertToDatabaseColumn("ABCDE1234F");
    assertThat(converter.convertToEntityAttribute(db)).isEqualTo("ABCDE1234F");
  }
}
