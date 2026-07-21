package com.paywithease.ocr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "documents")
public class DocumentRecord {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "storage_key", nullable = false)
  private String storageKey;

  @Column(name = "original_filename", nullable = false)
  private String originalFilename;

  @Column(name = "mime_type", nullable = false)
  private String mimeType;

  @Column private String checksum;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "uploaded_by", length = 26, columnDefinition = "char(26)")
  private String uploadedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected DocumentRecord() {}

  public DocumentRecord(
      String id,
      String tenantId,
      String storageKey,
      String originalFilename,
      String mimeType,
      String checksum,
      long sizeBytes,
      String uploadedBy,
      Instant createdAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.storageKey = storageKey;
    this.originalFilename = originalFilename;
    this.mimeType = mimeType;
    this.checksum = checksum;
    this.sizeBytes = sizeBytes;
    this.uploadedBy = uploadedBy;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getChecksum() {
    return checksum;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getUploadedBy() {
    return uploadedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
