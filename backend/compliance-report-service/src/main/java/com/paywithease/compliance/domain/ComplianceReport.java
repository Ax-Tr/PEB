package com.paywithease.compliance.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A generated compliance report governed by the {@link ComplianceStatus} lifecycle. Enforces the
 * product rules: a report cannot be APPROVED until its data is reconciled, and can only reach FILED
 * with an official acknowledgement reference.
 */
@Entity
@Table(name = "compliance_reports")
public class ComplianceReport {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private int year;

  @Column(nullable = false)
  private int month;

  @Column(nullable = false)
  private String status;

  @Column(name = "data_reconciled", nullable = false)
  private boolean dataReconciled;

  @Column(name = "total_taxable_minor", nullable = false)
  private long totalTaxableMinor;

  @Column(name = "total_tax_minor", nullable = false)
  private long totalTaxMinor;

  @Column(name = "net_payable_minor", nullable = false)
  private long netPayableMinor;

  @Column(name = "missing_fields", columnDefinition = "jsonb", nullable = false)
  private String missingFields;

  @Column(name = "ack_reference")
  private String ackReference;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  @Column(name = "reviewed_by", length = 26)
  private String reviewedBy;

  @Column(name = "approved_by", length = 26)
  private String approvedBy;

  @Column(name = "filed_at")
  private Instant filedAt;

  @Version private long version;

  protected ComplianceReport() {}

  public ComplianceReport(
      String id, String tenantId, ReportType type, int year, int month, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.type = type.name();
    this.year = year;
    this.month = month;
    this.status = ComplianceStatus.DRAFT.name();
    this.dataReconciled = false;
    this.missingFields = "[]";
    this.generatedAt = now;
  }

  public void setTotals(long taxable, long tax, long netPayable, String missingFieldsJson) {
    this.totalTaxableMinor = taxable;
    this.totalTaxMinor = tax;
    this.netPayableMinor = netPayable;
    this.missingFields = missingFieldsJson == null ? "[]" : missingFieldsJson;
  }

  public void setReconciled(boolean reconciled) {
    this.dataReconciled = reconciled;
  }

  public void markReviewed(String actorId) {
    requireStatus(ComplianceStatus.DRAFT, "Only a draft report can be reviewed");
    this.status = ComplianceStatus.REVIEWED.name();
    this.reviewedBy = actorId;
  }

  /** Approve — only allowed once the underlying data is reconciled (product rule). */
  public void approve(String actorId) {
    requireStatus(ComplianceStatus.REVIEWED, "Only a reviewed report can be approved");
    if (!dataReconciled) {
      throw new ApiException(
          ErrorCode.CONFLICT, "Data must be reconciled before the report can be approved");
    }
    this.status = ComplianceStatus.APPROVED.name();
    this.approvedBy = actorId;
  }

  /** The ONLY path to FILED: an official portal/API acknowledgement must be supplied. */
  public void recordFiling(String ackReference, Instant now) {
    requireStatus(ComplianceStatus.APPROVED, "Only an approved report can be filed");
    if (ackReference == null || ackReference.isBlank()) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "An official acknowledgement reference is required to mark filed");
    }
    this.status = ComplianceStatus.FILED.name();
    this.ackReference = ackReference;
    this.filedAt = now;
  }

  private void requireStatus(ComplianceStatus expected, String message) {
    if (!expected.name().equals(status)) {
      throw new ApiException(ErrorCode.CONFLICT, message + " (current: " + status + ")");
    }
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

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public String getStatus() {
    return status;
  }

  public boolean isDataReconciled() {
    return dataReconciled;
  }

  public long getTotalTaxableMinor() {
    return totalTaxableMinor;
  }

  public long getTotalTaxMinor() {
    return totalTaxMinor;
  }

  public long getNetPayableMinor() {
    return netPayableMinor;
  }

  public String getMissingFields() {
    return missingFields;
  }

  public String getAckReference() {
    return ackReference;
  }

  /**
   * UI display state: "unreconciled" surfaces when data isn't reconciled yet (still
   * DRAFT/REVIEWED).
   */
  public String displayState() {
    if (!dataReconciled
        && (ComplianceStatus.DRAFT.name().equals(status)
            || ComplianceStatus.REVIEWED.name().equals(status))) {
      return "UNRECONCILED";
    }
    return status;
  }
}
