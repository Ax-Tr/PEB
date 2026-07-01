package com.paywithease.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MobileNumberTest {

  @Test
  void normalizesCommonForms() {
    assertThat(MobileNumber.of("9876543210").value()).isEqualTo("9876543210");
    assertThat(MobileNumber.of("+91 98765-43210").value()).isEqualTo("9876543210");
    assertThat(MobileNumber.of("919876543210").value()).isEqualTo("9876543210");
    assertThat(MobileNumber.of("09876543210").value()).isEqualTo("9876543210");
  }

  @Test
  void rejectsInvalid() {
    assertThat(MobileNumber.isValid("1234567890")).isFalse(); // must start 6-9
    assertThat(MobileNumber.isValid("98765")).isFalse();
    assertThat(MobileNumber.isValid(null)).isFalse();
    assertThatThrownBy(() -> MobileNumber.of("abc")).isInstanceOf(IllegalArgumentException.class);
  }
}
