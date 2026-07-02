package com.paywithease.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A business expense that goes through maker-checker approval. Status is a String: {@code
 * PENDING_APPROVAL} on creation, {@code APPROVED} once a checker approves it. Amounts are integer
 * paise.
 */
@Entity
@Table(name = "expenses")
public class Expense {

  /** Expense lifecycle states (kept as String on the row per schema). */
  public static final String PENDING_APPROVAL = "PENDING_APPROVAL";

  public static final String APPROVED = "APPROVED";

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "category", nullable = false)
  private String category;

  @Column(name = "description")
  private String description;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal gstRate;

  @Column(name = "input_gst_minor", nullable = false)
  private long inputGstMinor;

  @Column(name = "vendor_id", length = 26)
  private String vendorId;

  @Column(name = "expense_date", nullable = false)
  private LocalDate expenseDate;

  @Column(name = "status", nullable = false)
  private String status = PENDING_APPROVAL;

  @Column(name = "approved_by", length = 26)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected Expense() {}

  public Expense(
      String id,
      String tenantId,
      String category,
      String description,
      long amountMinor,
      BigDecimal gstRate,
      long inputGstMinor,
      String vendorId,
      LocalDate expenseDate,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.category = category;
    this.description = description;
    this.amountMinor = amountMinor;
    this.gstRate = gstRate;
    this.inputGstMinor = inputGstMinor;
    this.vendorId = vendorId;
    this.expenseDate = expenseDate;
    this.status = PENDING_APPROVAL;
    this.createdAt = now;
  }

  /** Approve this expense: transition to APPROVED and record the checker and time. */
  public void approve(String approverId, Instant now) {
    this.status = APPROVED;
    this.approvedBy = approverId;
    this.approvedAt = now;
  }

  public boolean isPendingApproval() {
    return PENDING_APPROVAL.equals(status);
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public BigDecimal getGstRate() {
    return gstRate;
  }

  public long getInputGstMinor() {
    return inputGstMinor;
  }

  public String getVendorId() {
    return vendorId;
  }

  public LocalDate getExpenseDate() {
    return expenseDate;
  }

  public String getStatus() {
    return status;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public long getVersion() {
    return version;
  }
}
