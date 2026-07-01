package com.paywithease.ledger.domain;

/**
 * Financial period lifecycle (see specs/accounting-chart-of-accounts.md §4): {@code OPEN →
 * DRAFT_CLOSED → LOCKED → AUDITED}, with a maker-checker REOPEN back to OPEN. Posting is blocked
 * once a period is LOCKED (or AUDITED) unless an approved reopen occurs.
 */
public enum PeriodState {
  OPEN,
  DRAFT_CLOSED,
  LOCKED,
  AUDITED;

  public boolean postingAllowed() {
    return this == OPEN || this == DRAFT_CLOSED;
  }
}
