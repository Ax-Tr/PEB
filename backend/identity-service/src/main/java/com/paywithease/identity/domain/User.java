package com.paywithease.identity.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A person who can authenticate. Identified by mobile (encrypted at rest, looked up by blind
 * index).
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(columnDefinition = "char(26)")
  private String id;

  @Column(name = "tenant_id", columnDefinition = "char(26)")
  private String tenantId;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "mobile_enc", nullable = false)
  private String mobile;

  @Column(name = "mobile_hash", columnDefinition = "char(64)", nullable = false)
  private String mobileHash;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "email_enc")
  private String email;

  @Column(name = "display_name")
  private String displayName;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected User() {}

  public User(String id, String mobile, String mobileHash, Instant now) {
    this.id = id;
    this.mobile = mobile;
    this.mobileHash = mobileHash;
    this.status = "ACTIVE";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void attachTenant(String tenantId, Instant now) {
    this.tenantId = tenantId;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getMobile() {
    return mobile;
  }

  public String getMobileHash() {
    return mobileHash;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getStatus() {
    return status;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
