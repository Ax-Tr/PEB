package com.paywithease.analytics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "fact_commitments")
public class FactCommitment {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "commitment_id", length = 26, columnDefinition = "char(26)")
  private String commitmentId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "counterparty_type", nullable = false)
  private String counterpartyType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "counterparty_id", length = 26, columnDefinition = "char(26)")
  private String counterpartyId;

  @Column(name = "counterparty_name")
  private String counterpartyName;

  @Column(name = "source_type")
  private String sourceType;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "paid_minor", nullable = false)
  private long paidMinor;

  @Column(name = "outstanding_minor", nullable = false)
  private long outstandingMinor;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FactCommitment() {}

  public FactCommitment(
      String commitmentId,
      String tenantId,
      String counterpartyType,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      LocalDate dueDate,
      long amountMinor,
      long paidMinor,
      long outstandingMinor,
      String status,
      Instant now) {
    this.commitmentId = commitmentId;
    this.tenantId = tenantId;
    this.createdAt = now;
    update(
        counterpartyType,
        counterpartyId,
        counterpartyName,
        sourceType,
        dueDate,
        amountMinor,
        paidMinor,
        outstandingMinor,
        status,
        now);
  }

  public void update(
      String counterpartyType,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      LocalDate dueDate,
      long amountMinor,
      long paidMinor,
      long outstandingMinor,
      String status,
      Instant now) {
    this.counterpartyType = counterpartyType;
    this.counterpartyId = counterpartyId;
    this.counterpartyName = counterpartyName;
    this.sourceType = sourceType;
    this.dueDate = dueDate;
    this.amountMinor = amountMinor;
    this.paidMinor = paidMinor;
    this.outstandingMinor = outstandingMinor;
    this.status = status;
    this.updatedAt = now;
  }

  public String getCommitmentId() {
    return commitmentId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getCounterpartyType() {
    return counterpartyType;
  }

  public String getCounterpartyId() {
    return counterpartyId;
  }

  public String getCounterpartyName() {
    return counterpartyName;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public long getPaidMinor() {
    return paidMinor;
  }

  public long getOutstandingMinor() {
    return outstandingMinor;
  }

  public String getStatus() {
    return status;
  }
}
