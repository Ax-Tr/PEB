package com.paywithease.ai.infrastructure;

import com.paywithease.ai.domain.ParsedVoiceIntent;
import com.paywithease.ai.domain.VoiceDraftStatus;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "voice_drafts")
public class VoiceDraft {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "transcript_enc", nullable = false)
  private String transcript;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "sanitized_enc", nullable = false)
  private String sanitizedTranscript;

  @Column(nullable = false)
  private String intent;

  @Column(nullable = false)
  private String status;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "fields_enc", nullable = false)
  private String fieldsJson;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "missing_fields_enc", nullable = false)
  private String missingFieldsJson;

  @Column(nullable = false)
  private BigDecimal confidence;

  @Column(nullable = false)
  private boolean suspicious;

  @Column(name = "materialized_ref", length = 26)
  private String materializedRef;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "created_by", length = 26)
  private String createdBy;

  @Column(name = "reviewed_by", length = 26)
  private String reviewedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  protected VoiceDraft() {}

  public VoiceDraft(
      String id,
      String tenantId,
      String transcript,
      String sanitizedTranscript,
      ParsedVoiceIntent parsed,
      String fieldsJson,
      String missingFieldsJson,
      boolean suspicious,
      String createdBy,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.transcript = transcript;
    this.sanitizedTranscript = sanitizedTranscript;
    this.intent = parsed.intent().name();
    this.status = VoiceDraftStatus.NEEDS_REVIEW.name();
    this.fieldsJson = fieldsJson;
    this.missingFieldsJson = missingFieldsJson;
    this.confidence = BigDecimal.valueOf(parsed.confidence());
    this.suspicious = suspicious;
    this.createdBy = createdBy;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void approve(String materializedRef, String actorId, Instant now) {
    if (!VoiceDraftStatus.NEEDS_REVIEW.name().equals(status)) {
      return;
    }
    if (suspicious) {
      throw new ApiException(ErrorCode.CONFLICT, "Suspicious voice drafts cannot be approved");
    }
    this.materializedRef = materializedRef;
    this.status = VoiceDraftStatus.APPROVED.name();
    this.reviewedBy = actorId;
    this.reviewedAt = now;
    this.updatedAt = now;
  }

  public void reject(String reason, String actorId, Instant now) {
    if (!VoiceDraftStatus.NEEDS_REVIEW.name().equals(status)) {
      return;
    }
    this.rejectionReason = reason;
    this.status = VoiceDraftStatus.REJECTED.name();
    this.reviewedBy = actorId;
    this.reviewedAt = now;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getTranscript() {
    return transcript;
  }

  public String getSanitizedTranscript() {
    return sanitizedTranscript;
  }

  public String getIntent() {
    return intent;
  }

  public String getStatus() {
    return status;
  }

  public String getFieldsJson() {
    return fieldsJson;
  }

  public String getMissingFieldsJson() {
    return missingFieldsJson;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public boolean isSuspicious() {
    return suspicious;
  }

  public String getMaterializedRef() {
    return materializedRef;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }
}
