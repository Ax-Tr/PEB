package com.paywithease.ai.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Human feedback on an AI suggestion — keeps the loop auditable and lets models be tuned. */
@Entity
@Table(name = "ai_feedback")
public class AiFeedback {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "suggestion_id", length = 26, nullable = false)
  private String suggestionId;

  @Column(nullable = false)
  private boolean helpful;

  @Column private String note;

  @Column(name = "given_by", length = 26, nullable = false)
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
