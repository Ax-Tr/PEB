package com.paywithease.ocr.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ocr_jobs")
public class OcrJob {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "document_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String documentId;

  @Column(name = "document_type", nullable = false)
  private String documentType;

  @Column(nullable = false)
  private String status;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "raw_text_enc")
  private String rawText;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "extracted_fields_enc")
  private String extractedFieldsJson;

  @Column(nullable = false)
  private BigDecimal confidence;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "reviewed_by", length = 26, columnDefinition = "char(26)")
  private String reviewedBy;

  protected OcrJob() {}

  public OcrJob(
      String id, String tenantId, String documentId, DocumentType documentType, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.documentId = documentId;
    this.documentType = documentType.name();
    this.status = OcrJobStatus.QUEUED.name();
    this.confidence = BigDecimal.ZERO;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void applyExtraction(
      String rawText, String extractedFieldsJson, BigDecimal confidence, Instant now) {
    this.rawText = rawText;
    this.extractedFieldsJson = extractedFieldsJson;
    this.confidence = confidence;
    this.status = OcrJobStatus.REVIEW_REQUIRED.name();
    this.updatedAt = now;
  }

  public void fail(String reason, Instant now) {
    this.failureReason = reason;
    this.status = OcrJobStatus.FAILED.name();
    this.updatedAt = now;
  }

  public void review(boolean accepted, String reviewedFieldsJson, String actorId, Instant now) {
    this.extractedFieldsJson =
        reviewedFieldsJson == null ? this.extractedFieldsJson : reviewedFieldsJson;
    this.status = accepted ? OcrJobStatus.COMPLETED.name() : OcrJobStatus.FAILED.name();
    this.reviewedAt = now;
    this.reviewedBy = actorId;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getDocumentId() {
    return documentId;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getStatus() {
    return status;
  }

  public String getRawText() {
    return rawText;
  }

  public String getExtractedFieldsJson() {
    return extractedFieldsJson;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public String getFailureReason() {
    return failureReason;
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
