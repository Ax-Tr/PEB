package com.paywithease.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable monetary value stored as integer <b>paise</b> (₹1 = 100 paise). Financial correctness
 * rule (ADR-0005): never use floating point for money. All arithmetic is exact integer arithmetic;
 * conversion to/from rupees uses banker's rounding (HALF_EVEN) at the presentation boundary only.
 */
public final class Money implements Comparable<Money> {

  public static final Money ZERO = new Money(0L);
  private static final int PAISE_PER_RUPEE = 100;

  private final long minor; // paise

  private Money(long minor) {
    this.minor = minor;
  }

  /** Construct from an integer paise (minor unit) amount. */
  public static Money ofMinor(long paise) {
    return new Money(paise);
  }

  /** Construct from a rupee amount; rounds to the nearest paise using banker's rounding. */
  public static Money ofRupees(BigDecimal rupees) {
    Objects.requireNonNull(rupees, "rupees");
    long paise = rupees.movePointRight(2).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    return new Money(paise);
  }

  public long toMinor() {
    return minor;
  }

  public BigDecimal toRupees() {
    return BigDecimal.valueOf(minor, 2);
  }

  public Money plus(Money other) {
    return new Money(Math.addExact(this.minor, other.minor));
  }

  public Money minus(Money other) {
    return new Money(Math.subtractExact(this.minor, other.minor));
  }

  public Money times(long factor) {
    return new Money(Math.multiplyExact(this.minor, factor));
  }

  /** Multiply by a rate (e.g. a GST percentage as a fraction) with banker's rounding to paise. */
  public Money percent(BigDecimal ratePercent) {
    BigDecimal result =
        toRupees().multiply(ratePercent).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    return ofRupees(result);
  }

  public boolean isNegative() {
    return minor < 0;
  }

  public boolean isZero() {
    return minor == 0;
  }

  @Override
  public int compareTo(Money o) {
    return Long.compare(this.minor, o.minor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Money money)) return false;
    return minor == money.minor;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(minor);
  }

  /** Display form, e.g. {@code ₹1,234.50}. Never used for calculations. */
  @Override
  public String toString() {
    return String.format("₹%,.2f", toRupees());
  }
}
