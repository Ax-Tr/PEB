package com.paywithease.auditevidence.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * An auditor export job with a small state machine: REQUESTED → PROCESSING → COMPLETED | FAILED.
 */
@Entity
@Table(name = "export_jobs")
public class ExportJob {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String scope;

  @Column(nullable = false)
  private String status;

  @Column(name = "requested_by", length = 26, nullable = false)
  private String requestedBy;

  @Column(name = "result_ref")
  private String resultRef;

  @Column private String error;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected ExportJob() {}

  public ExportJob(String id, String tenantId, String scope, String requestedBy, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.scope = scope;
    this.status = ExportStatus.REQUESTED.name();
    this.requestedBy = requestedBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void start(Instant now) {
    requireStatus(ExportStatus.REQUESTED, "Only a requested export can start");
    this.status = ExportStatus.PROCESSING.name();
    this.updatedAt = now;
  }

  public void complete(String resultRef, Instant now) {
    requireStatus(ExportStatus.PROCESSING, "Only a processing export can complete");
    this.status = ExportStatus.COMPLETED.name();
    this.resultRef = resultRef;
    this.updatedAt = now;
  }

  public void fail(String error, Instant now) {
    if (ExportStatus.COMPLETED.name().equals(status)) {
      throw new ApiException(ErrorCode.CONFLICT, "A completed export cannot be failed");
    }
    this.status = ExportStatus.FAILED.name();
    this.error = error;
    this.updatedAt = now;
  }

  private void requireStatus(ExportStatus expected, String message) {
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

  public String getScope() {
    return scope;
  }

  public String getStatus() {
    return status;
  }

  public String getResultRef() {
    return resultRef;
  }

  public String getError() {
    return error;
  }
}
