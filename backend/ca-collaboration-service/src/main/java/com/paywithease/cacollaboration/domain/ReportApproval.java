package com.paywithease.cacollaboration.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A maker-checker report approval. The requester and the approver must be different people, and a
 * request can only be decided once — a second decision is a conflict, not a silent overwrite.
 */
@Entity
@Table(name = "report_approvals")
public class ReportApproval {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "report_type", nullable = false)
  private String reportType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "report_ref", length = 26, nullable = false, columnDefinition = "char(26)")
  private String reportRef;

  @Column(nullable = false)
  private String status;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "requested_by", length = 26, nullable = false, columnDefinition = "char(26)")
  private String requestedBy;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "decided_by", length = 26, columnDefinition = "char(26)")
  private String decidedBy;

  @Column(name = "decision_note")
  private String decisionNote;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Version private long version;

  protected ReportApproval() {}

  public ReportApproval(
      String id,
      String tenantId,
      String reportType,
      String reportRef,
      String requestedBy,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.reportType = reportType;
    this.reportRef = reportRef;
    this.status = ApprovalStatus.REQUESTED.name();
    this.requestedBy = requestedBy;
    this.requestedAt = now;
  }

  public void decide(String approver, boolean approved, String note, Instant now) {
    if (!ApprovalStatus.REQUESTED.name().equals(status)) {
      throw new ApiException(
          ErrorCode.CONFLICT, "Approval already decided (" + status + "); cannot decide again");
    }
    if (approver == null || approver.equals(requestedBy)) {
      throw new ApiException(
          ErrorCode.FORBIDDEN, "Maker-checker: the approver must be different from the requester");
    }
    this.status = (approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED).name();
    this.decidedBy = approver;
    this.decisionNote = note;
    this.decidedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getReportType() {
    return reportType;
  }

  public String getReportRef() {
    return reportRef;
  }

  public String getStatus() {
    return status;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public String getDecidedBy() {
    return decidedBy;
  }
}
