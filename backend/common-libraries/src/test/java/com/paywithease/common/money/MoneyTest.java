package com.paywithease.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void constructsFromRupeesWithBankersRounding() {
    assertThat(Money.ofRupees(new BigDecimal("1234.505")).toMinor())
        .isEqualTo(123450L); // HALF_EVEN
    assertThat(Money.ofRupees(new BigDecimal("1234.515")).toMinor()).isEqualTo(123452L);
    assertThat(Money.ofRupees(new BigDecimal("100.00")).toMinor()).isEqualTo(10000L);
  }

  @Test
  void addsAndSubtractsExactly() {
    Money a = Money.ofMinor(10050);
    Money b = Money.ofMinor(4950);
    assertThat(a.plus(b).toMinor()).isEqualTo(15000L);
    assertThat(a.minus(b).toMinor()).isEqualTo(5100L);
  }

  @Test
  void computesGstPercentToPaise() {
    // 18% GST on ₹1000.00 = ₹180.00
    Money base = Money.ofRupees(new BigDecimal("1000.00"));
    assertThat(base.percent(new BigDecimal("18")).toMinor()).isEqualTo(18000L);
    // 9% CGST on ₹999.99 = ₹90.00 (89.9991 -> 90.00 HALF_EVEN)
    Money odd = Money.ofRupees(new BigDecimal("999.99"));
    assertThat(odd.percent(new BigDecimal("9")).toMinor()).isEqualTo(9000L);
  }

  @Test
  void detectsSignAndZero() {
    assertThat(Money.ZERO.isZero()).isTrue();
    assertThat(Money.ofMinor(-1).isNegative()).isTrue();
    assertThat(Money.ofMinor(1).isNegative()).isFalse();
  }

  @Test
  void overflowThrows() {
    Money max = Money.ofMinor(Long.MAX_VALUE);
    assertThatThrownBy(() -> max.plus(Money.ofMinor(1))).isInstanceOf(ArithmeticException.class);
  }

  @Test
  void formatsForDisplay() {
    assertThat(Money.ofMinor(123450).toString()).isEqualTo("₹1,234.50");
  }

  @Test
  void equalityByValue() {
    assertThat(Money.ofMinor(500)).isEqualTo(Money.ofMinor(500));
    assertThat(Money.ofMinor(500)).isNotEqualTo(Money.ofMinor(501));
    assertThat(Money.ofMinor(500).compareTo(Money.ofMinor(600))).isNegative();
  }
}
