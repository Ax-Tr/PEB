package com.paywithease.ledger.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** A tenant's accounting month with its lock state (see {@link PeriodState}). */
@Entity
@Table(name = "financial_periods")
public class FinancialPeriod {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private int year;

  @Column(nullable = false)
  private int month;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PeriodState state;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Version private long version;

  protected FinancialPeriod() {}

  public FinancialPeriod(String id, String tenantId, int year, int month, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.year = year;
    this.month = month;
    this.state = PeriodState.OPEN;
    this.openedAt = now;
  }

  public void lock(Instant now) {
    if (state == PeriodState.LOCKED || state == PeriodState.AUDITED) {
      throw new ApiException(ErrorCode.CONFLICT, "Period already locked");
    }
    this.state = PeriodState.LOCKED;
    this.lockedAt = now;
  }

  /** Reopen a locked period (caller must have satisfied the maker-checker approval). */
  public void reopen() {
    if (state != PeriodState.LOCKED) {
      throw new ApiException(ErrorCode.CONFLICT, "Only a locked period can be reopened");
    }
    this.state = PeriodState.OPEN;
    this.lockedAt = null;
  }

  public void draftClose() {
    if (state == PeriodState.OPEN) {
      this.state = PeriodState.DRAFT_CLOSED;
    }
  }

  public boolean postingAllowed() {
    return state.postingAllowed();
  }

  public String getId() {
    return id;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public PeriodState getState() {
    return state;
  }
}
