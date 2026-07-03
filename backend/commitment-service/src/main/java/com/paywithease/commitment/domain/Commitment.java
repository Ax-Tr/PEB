package com.paywithease.commitment.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/** A payment promise with due-date and outstanding-balance tracking. */
@Entity
@Table(name = "commitments")
public class Commitment {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "counterparty_type", nullable = false)
  private String counterpartyType;

  @Column(name = "counterparty_id", length = 26)
  private String counterpartyId;

  @Column(name = "counterparty_name")
  private String counterpartyName;

  @Column(name = "source_type", nullable = false)
  private String sourceType;

  @Column(name = "source_ref", length = 26)
  private String sourceRef;

  @Column private String description;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "paid_minor", nullable = false)
  private long paidMinor;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_by", length = 26)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Version private long version;

  protected Commitment() {}

  public Commitment(
      String id,
      String tenantId,
      CounterpartyType counterpartyType,
      String counterpartyId,
      String counterpartyName,
      SourceType sourceType,
      String sourceRef,
      String description,
      long amountMinor,
      LocalDate dueDate,
      String createdBy,
      Instant now) {
    if (amountMinor <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "amount must be positive");
    }
    if (dueDate == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "due date is required");
    }
    this.id = id;
    this.tenantId = tenantId;
    this.counterpartyType = counterpartyType.name();
    this.counterpartyId = counterpartyId;
    this.counterpartyName = counterpartyName;
    this.sourceType = sourceType.name();
    this.sourceRef = sourceRef;
    this.description = description;
    this.amountMinor = amountMinor;
    this.paidMinor = 0;
    this.dueDate = dueDate;
    this.status = CommitmentStatus.PROMISED.name();
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public long recordPayment(long amountMinor, Instant now) {
    ensureOpen("record payment");
    if (amountMinor <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "payment amount must be positive");
    }
    long outstanding = outstandingMinor();
    if (outstanding == 0) {
      throw new ApiException(ErrorCode.CONFLICT, "Commitment is already paid");
    }
    long applied = Math.min(amountMinor, outstanding);
    this.paidMinor += applied;
    this.updatedAt = now;
    if (this.paidMinor == this.amountMinor) {
      this.status = CommitmentStatus.PAID.name();
      this.closedAt = now;
    } else {
      this.status = CommitmentStatus.PARTIALLY_PAID.name();
    }
    return applied;
  }

  public LocalDate reschedule(LocalDate newDueDate, String note, Instant now) {
    ensureOpen("reschedule");
    if (newDueDate == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "new due date is required");
    }
    LocalDate old = this.dueDate;
    this.dueDate = newDueDate;
    this.description = note == null || note.isBlank() ? this.description : note;
    this.status =
        this.paidMinor > 0
            ? CommitmentStatus.PARTIALLY_PAID.name()
            : CommitmentStatus.RESCHEDULED.name();
    this.updatedAt = now;
    return old;
  }

  public void markBroken(Instant now) {
    ensureOpen("mark broken");
    this.status = CommitmentStatus.BROKEN.name();
    this.updatedAt = now;
  }

  public void cancel(String note, Instant now) {
    ensureOpen("cancel");
    this.status = CommitmentStatus.CANCELLED.name();
    this.description = note == null || note.isBlank() ? this.description : note;
    this.updatedAt = now;
    this.closedAt = now;
  }

  public boolean isOpen() {
    return !CommitmentStatus.PAID.name().equals(status)
        && !CommitmentStatus.CANCELLED.name().equals(status);
  }

  public long outstandingMinor() {
    return Math.max(0, amountMinor - paidMinor);
  }

  private void ensureOpen(String action) {
    if (!isOpen()) {
      throw new ApiException(
          ErrorCode.CONFLICT, "Cannot " + action + " when commitment is " + status);
    }
  }

  public String getId() {
    return id;
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

  public String getSourceType() {
    return sourceType;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getDescription() {
    return description;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public long getPaidMinor() {
    return paidMinor;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
