package com.paywithease.ai.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Human feedback on an AI suggestion — keeps the loop auditable and lets models be tuned. */
@Entity
@Table(name = "ai_feedback")
public class AiFeedback {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "suggestion_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String suggestionId;

  @Column(nullable = false)
  private boolean helpful;

  @Column private String note;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "given_by", length = 26, nullable = false, columnDefinition = "char(26)")
  private String givenBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AiFeedback() {}

  public AiFeedback(
      String id,
      String tenantId,
      String suggestionId,
      boolean helpful,
      String note,
      String givenBy,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.suggestionId = suggestionId;
    this.helpful = helpful;
    this.note = note;
    this.givenBy = givenBy;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }

  public String getSuggestionId() {
    return suggestionId;
  }

  public boolean isHelpful() {
    return helpful;
  }
}
