package com.paywithease.payout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A verified payout destination for a vendor/employee. {@code verifiedAt} records when the
 * destination was last verified or changed — a recently changed beneficiary is treated as
 * higher-risk (the "vendor bank change before payout" control).
 */
@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "party_type", nullable = false)
  private String partyType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "party_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String partyId;

  private String label;

  @Column(name = "account_number_hash", length = 64, nullable = false)
  private String accountNumberHash;

  @Column(nullable = false)
  private String status;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Beneficiary() {}

  public Beneficiary(
      String id,
      String tenantId,
      PartyType partyType,
      String partyId,
      String label,
      String accountNumberHash,
      Instant verifiedAt,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.partyType = partyType.name();
    this.partyId = partyId;
    this.label = label;
    this.accountNumberHash = accountNumberHash;
    this.status = "ACTIVE";
    this.verifiedAt = verifiedAt;
    this.createdAt = now;
  }

  public boolean isActive() {
    return "ACTIVE".equals(status);
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getPartyType() {
    return partyType;
  }

  public String getPartyId() {
    return partyId;
  }

  public String getLabel() {
    return label;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }
}
