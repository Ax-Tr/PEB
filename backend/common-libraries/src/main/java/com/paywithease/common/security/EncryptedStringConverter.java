package com.paywithease.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Transparently encrypts a String column at rest with AES-GCM. Apply with {@code @Convert(converter
 * = EncryptedStringConverter.class)} on sensitive entity fields. The ciphertext is what the
 * database (and backups) ever see.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return FieldCrypto.cipher().encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return FieldCrypto.cipher().decrypt(dbData);
  }
}
