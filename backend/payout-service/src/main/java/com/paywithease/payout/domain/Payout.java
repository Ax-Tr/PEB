package com.paywithease.payout.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A payout to a beneficiary, guarded by maker-checker approval for high-risk/high-value cases. */
@Entity
@Table(name = "payouts")
public class Payout {

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

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "beneficiary_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String beneficiaryId;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  private String purpose;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PayoutStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_level", nullable = false)
  private RiskLevel riskLevel;

  private String provider;

  @Column(name = "provider_ref")
  private String providerRef;

  @Column(name = "gateway_attempts", nullable = false)
  private int gatewayAttempts;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "created_by", length = 26, columnDefinition = "char(26)")
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Payout() {}

  public Payout(
      String id,
      String tenantId,
      PartyType partyType,
      String partyId,
      String beneficiaryId,
      long amountMinor,
      String purpose,
      RiskLevel riskLevel,
      boolean requiresApproval,
      String createdBy,
      Instant now) {
    if (amountMinor <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "amount must be positive");
    }
    this.id = id;
    this.tenantId = tenantId;
    this.partyType = partyType.name();
    this.partyId = partyId;
    this.beneficiaryId = beneficiaryId;
    this.amountMinor = amountMinor;
    this.purpose = purpose;
    this.riskLevel = riskLevel;
    this.status = requiresApproval ? PayoutStatus.PENDING_APPROVAL : PayoutStatus.APPROVED;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public boolean requiresApproval() {
    return status == PayoutStatus.PENDING_APPROVAL;
  }

  public void approve(Instant now) {
    requireStatus(PayoutStatus.PENDING_APPROVAL, "Only a pending payout can be approved");
    this.status = PayoutStatus.APPROVED;
    this.updatedAt = now;
  }

  public void reject(Instant now) {
    requireStatus(PayoutStatus.PENDING_APPROVAL, "Only a pending payout can be rejected");
    this.status = PayoutStatus.REJECTED;
    this.updatedAt = now;
  }

  public void markInitiated(String provider, String providerRef, Instant now) {
    requireStatus(PayoutStatus.APPROVED, "Only an approved payout can be initiated");
    this.provider = provider;
    this.providerRef = providerRef;
    this.status = PayoutStatus.INITIATED;
    this.updatedAt = now;
  }

  public void markCompleted(Instant now) {
    this.status = PayoutStatus.COMPLETED;
    this.updatedAt = now;
  }

  public void markFailed(Instant now) {
    this.status = PayoutStatus.FAILED;
    this.updatedAt = now;
  }

  public void incrementAttempts() {
    this.gatewayAttempts++;
  }

  private void requireStatus(PayoutStatus expected, String message) {
    if (this.status != expected) {
      throw new ApiException(ErrorCode.CONFLICT, message + " (current: " + status + ")");
    }
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

  public String getBeneficiaryId() {
    return beneficiaryId;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public String getPurpose() {
    return purpose;
  }

  public PayoutStatus getStatus() {
    return status;
  }

  public RiskLevel getRiskLevel() {
    return riskLevel;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderRef() {
    return providerRef;
  }

  public String getCreatedBy() {
    return createdBy;
  }
}
