package com.paywithease.ai.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A governed AI suggestion. Carries its confidence and governance decision; humans accept/reject.
 */
@Entity
@Table(name = "ai_suggestions")
public class AiSuggestion {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(nullable = false)
  private String kind;

  @Column(name = "subject_type", nullable = false)
  private String subjectType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "subject_id", length = 26, columnDefinition = "char(26)")
  private String subjectId;

  @Column(nullable = false, columnDefinition = "jsonb")
  private String suggestion;

  @Column(nullable = false)
  private BigDecimal confidence;

  @Column(nullable = false)
  private String decision;

  @Column(nullable = false)
  private String status;

  @Column(name = "model_ref")
  private String modelRef;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "reviewed_by", length = 26, columnDefinition = "char(26)")
  private String reviewedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Version private long version;

  protected AiSuggestion() {}

  public AiSuggestion(
      String id,
      String tenantId,
      String kind,
      String subjectType,
      String subjectId,
      String suggestionJson,
      BigDecimal confidence,
      String decision,
      String status,
      String modelRef,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.kind = kind;
    this.subjectType = subjectType;
    this.subjectId = subjectId;
    this.suggestion = suggestionJson;
    this.confidence = confidence;
    this.decision = decision;
    this.status = status;
    this.modelRef = modelRef;
    this.createdAt = now;
  }

  public void accept(String reviewer, Instant now) {
    this.status = "ACCEPTED";
    this.reviewedBy = reviewer;
    this.decidedAt = now;
  }

  public void reject(String reviewer, Instant now) {
    this.status = "REJECTED";
    this.reviewedBy = reviewer;
    this.decidedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getKind() {
    return kind;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public String getSubjectId() {
    return subjectId;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }

  public String getDecision() {
    return decision;
  }

  public String getStatus() {
    return status;
  }

  public String getModelRef() {
    return modelRef;
  }
}
