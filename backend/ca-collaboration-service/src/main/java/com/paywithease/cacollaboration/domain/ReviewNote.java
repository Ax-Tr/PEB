package com.paywithease.cacollaboration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An append-only review comment left by a collaborator on some entity. */
@Entity
@Table(name = "review_notes")
public class ReviewNote {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "entity_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String entityId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "author_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String authorId;

  @Column(nullable = false)
  private String note;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ReviewNote() {}

  public ReviewNote(
      String id,
      String tenantId,
      String entityType,
      String entityId,
      String authorId,
      String note,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.entityType = entityType;
    this.entityId = entityId;
    this.authorId = authorId;
    this.note = note;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getAuthorId() {
    return authorId;
  }

  public String getNote() {
    return note;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
