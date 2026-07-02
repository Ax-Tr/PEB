package com.paywithease.privacy.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A Data Subject Request governed by a small state machine: RECEIVED → VERIFYING → IN_PROGRESS →
 * COMPLETED | REJECTED. The requester's identity must be verified before any data is acted on
 * (prevents an attacker exercising someone else's rights), and completion records honest evidence —
 * for erasure this includes what was retained under legal hold rather than deleted.
 */
@Entity
@Table(name = "dsr_requests")
public class DsrRequest {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String status;

  @Column(name = "subject_ref", length = 26)
  private String subjectRef;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "subject_email_enc", nullable = false)
  private String subjectEmail;

  @Column private String details;

  @Column(name = "erasure_plan", columnDefinition = "jsonb")
  private String erasurePlan;

  @Column(name = "resolution_note")
  private String resolutionNote;

  @Column(name = "evidence_ref")
  private String evidenceRef;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "due_at", nullable = false)
  private Instant dueAt;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "handled_by", length = 26)
  private String handledBy;

  @Version private long version;

  protected DsrRequest() {}

  public DsrRequest(
      String id,
      String tenantId,
      DsrType type,
      String subjectRef,
      String subjectEmail,
      String details,
      Instant now,
      Instant dueAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.type = type.name();
    this.status = DsrStatus.RECEIVED.name();
    this.subjectRef = subjectRef;
    this.subjectEmail = subjectEmail;
    this.details = details;
    this.receivedAt = now;
    this.dueAt = dueAt;
  }

  public void startVerification() {
    requireStatus(DsrStatus.RECEIVED, "Only a received request can start verification");
    this.status = DsrStatus.VERIFYING.name();
  }

  public void markVerified(String handledBy, Instant now) {
    requireStatus(
        DsrStatus.VERIFYING, "Requester identity must be verified from the VERIFYING state");
    this.status = DsrStatus.IN_PROGRESS.name();
    this.verifiedAt = now;
    this.handledBy = handledBy;
  }

  public void attachErasurePlan(String planJson) {
    if (!DsrStatus.IN_PROGRESS.name().equals(status)) {
      throw new ApiException(
          ErrorCode.CONFLICT, "An erasure plan can only be attached while in progress");
    }
    this.erasurePlan = planJson;
  }

  public void complete(String evidenceRef, String note, Instant now) {
    requireStatus(DsrStatus.IN_PROGRESS, "Only a verified, in-progress request can be completed");
    this.status = DsrStatus.COMPLETED.name();
    this.evidenceRef = evidenceRef;
    this.resolutionNote = note;
    this.completedAt = now;
  }

  public void reject(String reason, Instant now) {
    if (DsrStatus.COMPLETED.name().equals(status) || DsrStatus.REJECTED.name().equals(status)) {
      throw new ApiException(ErrorCode.CONFLICT, "Request is already " + status);
    }
    this.status = DsrStatus.REJECTED.name();
    this.resolutionNote = reason;
    this.completedAt = now;
  }

  public boolean isOverdue(Instant now) {
    boolean terminal =
        DsrStatus.COMPLETED.name().equals(status) || DsrStatus.REJECTED.name().equals(status);
    return !terminal && now.isAfter(dueAt);
  }

  private void requireStatus(DsrStatus expected, String message) {
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

  public String getStatus() {
    return status;
  }

  public String getSubjectRef() {
    return subjectRef;
  }

  public String getSubjectEmail() {
    return subjectEmail;
  }

  public String getDetails() {
    return details;
  }

  public String getErasurePlan() {
    return erasurePlan;
  }

  public String getResolutionNote() {
    return resolutionNote;
  }

  public String getEvidenceRef() {
    return evidenceRef;
  }

  public Instant getDueAt() {
    return dueAt;
  }
}
