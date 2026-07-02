package com.paywithease.reconciliation.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** A proposed or confirmed pairing between an external and an internal item. */
@Entity
@Table(name = "reconciliation_matches")
public class ReconMatch {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "external_item_id", length = 26, nullable = false)
  private String externalItemId;

  @Column(name = "internal_item_id", length = 26, nullable = false)
  private String internalItemId;

  @Column(nullable = false)
  private BigDecimal score;

  @Column(nullable = false)
  private String status; // AUTO, SUGGESTED, CONFIRMED, REJECTED

  @Column(name = "matched_by", length = 26)
  private String matchedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  protected ReconMatch() {}

  public ReconMatch(
      String id,
      String tenantId,
      String externalItemId,
      String internalItemId,
      BigDecimal score,
      String status,
      String matchedBy,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.externalItemId = externalItemId;
    this.internalItemId = internalItemId;
    this.score = score;
    this.status = status;
    this.matchedBy = matchedBy;
    this.createdAt = now;
    if ("AUTO".equals(status) || "CONFIRMED".equals(status)) {
      this.decidedAt = now;
    }
  }

  public void confirm(String actorId, Instant now) {
    requireStatus("SUGGESTED", "Only a suggested match can be confirmed");
    this.status = "CONFIRMED";
    this.matchedBy = actorId;
    this.decidedAt = now;
  }

  public void reject(String actorId, Instant now) {
    requireStatus("SUGGESTED", "Only a suggested match can be rejected");
    this.status = "REJECTED";
    this.matchedBy = actorId;
    this.decidedAt = now;
  }

  private void requireStatus(String expected, String message) {
    if (!expected.equals(status)) {
      throw new ApiException(ErrorCode.CONFLICT, message + " (current: " + status + ")");
    }
  }

  public boolean isEffective() {
    return "AUTO".equals(status) || "CONFIRMED".equals(status);
  }

  public String getId() {
    return id;
  }

  public String getExternalItemId() {
    return externalItemId;
  }

  public String getInternalItemId() {
    return internalItemId;
  }

  public String getStatus() {
    return status;
  }
}
