package com.paywithease.ocr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "documents")
public class DocumentRecord {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
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

  @Column(name = "uploaded_by", length = 26)
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
