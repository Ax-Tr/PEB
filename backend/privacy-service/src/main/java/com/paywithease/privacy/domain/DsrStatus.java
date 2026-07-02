package com.paywithease.privacy.domain;

/** Lifecycle of a data subject request. */
public enum DsrStatus {
  RECEIVED,
  VERIFYING,
  IN_PROGRESS,
  COMPLETED,
  REJECTED
}
