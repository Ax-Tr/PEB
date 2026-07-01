package com.paywithease.vendor.domain;

/**
 * Review lifecycle of a vendor bank account. A newly captured account is {@code PENDING_REVIEW} and
 * unusable for payouts until a user explicitly moves it to {@code VERIFIED} (product rule #7).
 */
public enum BankAccountStatus {
  PENDING_REVIEW,
  VERIFIED,
  REJECTED
}
