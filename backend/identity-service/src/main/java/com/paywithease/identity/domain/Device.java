package com.paywithease.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A device bound to a user (device binding for suspicious-login detection & step-up). */
@Entity
@Table(name = "devices")
public class Device {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "user_id", columnDefinition = "char(26)", nullable = false)
  private String userId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "device_hash", columnDefinition = "char(64)", nullable = false)
  private String deviceHash;

  private String platform;
  private String model;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  @Column(nullable = false)
  private boolean trusted;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Device() {}

  public Device(
      String id, String userId, String deviceHash, String platform, String model, Instant now) {
    this.id = id;
    this.userId = userId;
    this.deviceHash = deviceHash;
    this.platform = platform;
    this.model = model;
    this.lastSeenAt = now;
    this.createdAt = now;
    this.trusted = false;
  }

  public void touch(Instant now) {
    this.lastSeenAt = now;
  }

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getDeviceHash() {
    return deviceHash;
  }

  public boolean isTrusted() {
    return trusted;
  }
}
