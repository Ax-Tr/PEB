package com.paywithease.payment.domain;

/** Lifecycle of a payment request. */
public enum PaymentStatus {
  AWAITING_PAYMENT,
  PARTIALLY_PAID,
  PAID,
  FAILED,
  CANCELLED,
  EXPIRED
}
