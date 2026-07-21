package com.paywithease.installment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A single EMI within a schedule. */
@Entity
@Table(name = "installment_emis")
public class InstallmentEmi {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "installment_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String installmentId;

  @Column(name = "emi_number", nullable = false)
  private int emiNumber;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "paid_minor", nullable = false)
  private long paidMinor;

  @Column(nullable = false)
  private String status; // PENDING, PARTIAL, PAID

  @Column(name = "paid_at")
  private Instant paidAt;

  protected InstallmentEmi() {}

  public InstallmentEmi(
      String id,
      String tenantId,
      String installmentId,
      int emiNumber,
      LocalDate dueDate,
      long amountMinor) {
    this.id = id;
    this.tenantId = tenantId;
    this.installmentId = installmentId;
    this.emiNumber = emiNumber;
    this.dueDate = dueDate;
    this.amountMinor = amountMinor;
    this.status = "PENDING";
  }

  /** Applies up to the EMI's outstanding amount; returns how much was actually applied. */
  public long apply(long incomingMinor, Instant now) {
    long remaining = amountMinor - paidMinor;
    if (remaining <= 0) {
      return 0;
    }
    long applied = Math.min(incomingMinor, remaining);
    paidMinor += applied;
    if (paidMinor >= amountMinor) {
      status = "PAID";
      paidAt = now;
    } else {
      status = "PARTIAL";
    }
    return applied;
  }

  public long outstandingMinor() {
    return amountMinor - paidMinor;
  }

  public String getId() {
    return id;
  }

  public int getEmiNumber() {
    return emiNumber;
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

  public String getStatus() {
    return status;
  }

  public boolean isPaid() {
    return "PAID".equals(status);
  }
}
