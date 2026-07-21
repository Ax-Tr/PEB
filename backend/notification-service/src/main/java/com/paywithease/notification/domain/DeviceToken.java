package com.paywithease.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A registered push token for a user's device. Soft-revoked on logout (never hard-deleted). */
@Entity
@Table(name = "device_tokens")
public class DeviceToken {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "user_id", length = 26, columnDefinition = "char(26)")
  private String userId;

  @Column(nullable = false)
  private String token;

  @Column(nullable = false)
  private String platform; // ios | android | web

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected DeviceToken() {}

  public DeviceToken(
      String id, String tenantId, String userId, String token, String platform, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.userId = userId;
    this.token = token;
    this.platform = platform;
    this.active = true;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Re-registration of an existing token: reactivate and refresh its metadata. */
  public void touch(String userId, String platform, Instant now) {
    this.active = true;
    this.userId = userId;
    this.platform = platform;
    this.updatedAt = now;
  }

  public void revoke(Instant now) {
    this.active = false;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getToken() {
    return token;
  }

  public String getPlatform() {
    return platform;
  }

  public boolean isActive() {
    return active;
  }
}
