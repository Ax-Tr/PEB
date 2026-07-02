package com.paywithease.installment.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** A receivable/payable EMI schedule. Tracks outstanding balance and closes at zero. */
@Entity
@Table(name = "installments")
public class Installment {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String type;

  @Column(name = "counterparty_id", length = 26)
  private String counterpartyId;

  @Column(name = "counterparty_name")
  private String counterpartyName;

  @Column(name = "source_type")
  private String sourceType;

  @Column(name = "source_ref", length = 26)
  private String sourceRef;

  @Column(name = "total_amount_minor", nullable = false)
  private long totalAmountMinor;

  @Column(name = "outstanding_minor", nullable = false)
  private long outstandingMinor;

  @Column(name = "number_of_emis", nullable = false)
  private int numberOfEmis;

  @Column(nullable = false)
  private String frequency;

  @Column(nullable = false)
  private String status; // ACTIVE, CLOSED, CANCELLED

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Installment() {}

  public Installment(
      String id,
      String tenantId,
      InstallmentType type,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      long totalAmountMinor,
      int numberOfEmis,
      Frequency frequency,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.type = type.name();
    this.counterpartyId = counterpartyId;
    this.counterpartyName = counterpartyName;
    this.sourceType = sourceType;
    this.sourceRef = sourceRef;
    this.totalAmountMinor = totalAmountMinor;
    this.outstandingMinor = totalAmountMinor;
    this.numberOfEmis = numberOfEmis;
    this.frequency = frequency.name();
    this.status = "ACTIVE";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void reduceOutstanding(long appliedMinor, Instant now) {
    if (appliedMinor <= 0) {
      return;
    }
    this.outstandingMinor = Math.max(0, this.outstandingMinor - appliedMinor);
    this.updatedAt = now;
    if (this.outstandingMinor == 0) {
      this.status = "CLOSED";
    }
  }

  public void cancel(Instant now) {
    if (!"ACTIVE".equals(status)) {
      throw new ApiException(ErrorCode.CONFLICT, "Only an active schedule can be cancelled");
    }
    this.status = "CANCELLED";
    this.updatedAt = now;
  }

  public void touch(Instant now) {
    this.updatedAt = now;
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

  public String getType() {
    return type;
  }

  public String getCounterpartyId() {
    return counterpartyId;
  }

  public String getCounterpartyName() {
    return counterpartyName;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public long getTotalAmountMinor() {
    return totalAmountMinor;
  }

  public long getOutstandingMinor() {
    return outstandingMinor;
  }

  public int getNumberOfEmis() {
    return numberOfEmis;
  }

  public String getFrequency() {
    return frequency;
  }

  public String getStatus() {
    return status;
  }
}
