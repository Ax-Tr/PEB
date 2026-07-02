package com.paywithease.payout.domain;

/** Payout lifecycle. */
public enum PayoutStatus {
  PENDING_APPROVAL,
  APPROVED,
  INITIATED,
  COMPLETED,
  FAILED,
  REJECTED
}
