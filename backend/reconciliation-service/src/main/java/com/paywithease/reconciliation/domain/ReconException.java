package com.paywithease.reconciliation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** An unmatched item flagged for human attention. */
@Entity
@Table(name = "reconciliation_exceptions")
public class ReconException {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "item_id", length = 26, nullable = false)
  private String itemId;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private String status; // OPEN, RESOLVED

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  protected ReconException() {}

  public ReconException(String id, String tenantId, String itemId, String reason, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.itemId = itemId;
    this.reason = reason;
    this.status = "OPEN";
    this.createdAt = now;
  }

  public void resolve(Instant now) {
    this.status = "RESOLVED";
    this.resolvedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getItemId() {
    return itemId;
  }

  public String getReason() {
    return reason;
  }

  public String getStatus() {
    return status;
  }
}
