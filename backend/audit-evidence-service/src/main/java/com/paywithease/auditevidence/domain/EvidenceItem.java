package com.paywithease.auditevidence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An immutable piece of evidence linked to a business entity. There are deliberately no setters and
 * the service exposes no update/delete: evidence is append-only (financial-evidence rule), and the
 * database enforces the same via a trigger. The {@code contentHash} allows later integrity checks,
 * and the entity reference may point at a reversed transaction — the proof survives the reversal.
 */
@Entity
@Table(name = "evidence_items")
public class EvidenceItem {

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

  @Column(name = "content_hash", length = 64, nullable = false)
  private String contentHash;

  @Column(name = "storage_ref")
  private String storageRef;

  @Column private String description;

  @Column(nullable = false)
  private String source; // UPLOAD | SYSTEM_EVENT

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "uploaded_by", length = 26, columnDefinition = "char(26)")
  private String uploadedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected EvidenceItem() {}

  public EvidenceItem(
      String id,
      String tenantId,
      String entityType,
      String entityId,
      String contentHash,
      String storageRef,
      String description,
      String source,
      String uploadedBy,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.entityType = entityType;
    this.entityId = entityId;
    this.contentHash = contentHash;
    this.storageRef = storageRef;
    this.description = description;
    this.source = source;
    this.uploadedBy = uploadedBy;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getContentHash() {
    return contentHash;
  }

  public String getStorageRef() {
    return storageRef;
  }

  public String getDescription() {
    return description;
  }

  public String getSource() {
    return source;
  }

  public String getUploadedBy() {
    return uploadedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
