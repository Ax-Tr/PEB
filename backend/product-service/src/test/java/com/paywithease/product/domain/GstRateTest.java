package com.paywithease.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GstRateTest {

  @Test
  void allowsTheStandardGstSlabs() {
    assertThat(GstRate.isAllowed(new BigDecimal("0"))).isTrue();
    assertThat(GstRate.isAllowed(new BigDecimal("0.25"))).isTrue();
    assertThat(GstRate.isAllowed(new BigDecimal("3"))).isTrue();
    assertThat(GstRate.isAllowed(new BigDecimal("5"))).isTrue();
    assertThat(GstRate.isAllowed(new BigDecimal("12"))).isTrue();
    assertThat(GstRate.isAllowed(new BigDecimal("18"))).isTrue();
    assertThat(GstRate.isAllowed(new BigDecimal("28"))).isTrue();
    // scale-insensitive
    assertThat(GstRate.isAllowed(new BigDecimal("18.00"))).isTrue();
  }

  @Test
  void rejectsRatesOutsideTheSlabs() {
    assertThat(GstRate.isAllowed(new BigDecimal("7"))).isFalse();
    assertThat(GstRate.isAllowed(new BigDecimal("15"))).isFalse();
    assertThat(GstRate.isAllowed(new BigDecimal("100"))).isFalse();
    assertThat(GstRate.isAllowed(null)).isFalse();
  }

  @Test
  void ofParsesAllowedAndRejectsDisallowed() {
    assertThat(GstRate.of("18")).isEqualByComparingTo(new BigDecimal("18"));
    assertThatThrownBy(() -> GstRate.of("7")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> GstRate.of("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void itemTypeValidation() {
    assertThat(ItemType.isValid("GOOD")).isTrue();
    assertThat(ItemType.isValid("SERVICE")).isTrue();
    assertThat(ItemType.isValid("good")).isFalse();
    assertThat(ItemType.isValid("WIDGET")).isFalse();
    assertThat(ItemType.isValid(null)).isFalse();
  }
}
