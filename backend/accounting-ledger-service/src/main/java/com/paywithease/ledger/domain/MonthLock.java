package com.paywithease.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only lock/reopen action log (maker-checker evidence, alongside audit_events). */
@Entity
@Table(name = "month_locks")
public class MonthLock {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "period_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String periodId;

  @Column(nullable = false)
  private String action; // LOCK, REOPEN

  private String reason;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "actor_id", length = 26, columnDefinition = "char(26)")
  private String actorId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected MonthLock() {}

  public MonthLock(
      String id,
      String tenantId,
      String periodId,
      String action,
      String reason,
      String actorId,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.periodId = periodId;
    this.action = action;
    this.reason = reason;
    this.actorId = actorId;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }
}
