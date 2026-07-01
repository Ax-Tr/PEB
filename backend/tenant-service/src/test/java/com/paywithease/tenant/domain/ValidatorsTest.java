package com.paywithease.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ValidatorsTest {

  @Test
  void gstinChecksumRoundTrips() {
    // Build a GSTIN whose 15th char is the correct check digit, then verify it validates.
    String first14 = "27AAPFU0939F1Z";
    char check = Gstin.checkDigit(first14);
    String valid = first14 + check;
    assertThat(Gstin.isValid(valid)).isTrue();
    assertThat(Gstin.of(valid).stateCode()).isEqualTo("27");
    assertThat(Gstin.of(valid).pan()).isEqualTo("AAPFU0939F");
  }

  @Test
  void gstinRejectsBadChecksumAndFormat() {
    String first14 = "27AAPFU0939F1Z";
    char correct = Gstin.checkDigit(first14);
    char wrong = correct == 'A' ? 'B' : 'A';
    assertThat(Gstin.isValid(first14 + wrong)).isFalse();
    assertThat(Gstin.isValid("BADGSTIN")).isFalse();
    assertThat(Gstin.isValid(null)).isFalse();
    assertThatThrownBy(() -> Gstin.of("nope")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void panValidation() {
    assertThat(Pan.isValid("ABCDE1234F")).isTrue();
    assertThat(Pan.of("abcde1234f").value()).isEqualTo("ABCDE1234F");
    assertThat(Pan.of("AAPFU0939F").holderType()).isEqualTo('F');
    assertThat(Pan.isValid("ABCD1234F")).isFalse();
    assertThat(Pan.isValid("ABCDE12345")).isFalse();
  }

  @Test
  void udyamValidation() {
    assertThat(Udyam.isValid("UDYAM-MH-01-1234567")).isTrue();
    assertThat(Udyam.isValid("UDYAM-MH-1-1234567")).isFalse();
    assertThat(Udyam.isValid("MH-01-1234567")).isFalse();
  }

  @Test
  void businessTypeValidation() {
    assertThat(BusinessType.isValid("PROPRIETOR")).isTrue();
    assertThat(BusinessType.isValid("pvt")).isFalse();
    assertThat(BusinessType.isValid(null)).isFalse();
  }
}
