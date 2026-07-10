package com.paywithease.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A refresh-token session. Refresh tokens rotate: each use marks the old row {@code ROTATED} and
 * creates a new one in the same {@code familyId}. Reuse of an already-rotated token is treated as
 * theft and revokes the whole family.
 */
@Entity
@Table(name = "user_sessions")
public class UserSession {

  public enum Status {
    ACTIVE,
    ROTATED,
    REVOKED,
    EXPIRED
  }

  @Id
  @Column(columnDefinition = "char(26)")
  private String id;

  @Column(name = "user_id", columnDefinition = "char(26)", nullable = false)
  private String userId;

  @Column(name = "device_id", columnDefinition = "char(26)")
  private String deviceId;

  @Column(name = "refresh_token_hash", columnDefinition = "char(64)", nullable = false)
  private String refreshTokenHash;

  @Column(name = "family_id", columnDefinition = "char(26)", nullable = false)
  private String familyId;

  @Column(nullable = false)
  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  private Status status;

  private String ip;

  @Column(name = "user_agent")
  private String userAgent;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  protected UserSession() {}

  public UserSession(
      String id,
      String userId,
      String deviceId,
      String refreshTokenHash,
      String familyId,
      String ip,
      String userAgent,
      Instant issuedAt,
      Instant expiresAt) {
    this.id = id;
    this.userId = userId;
    this.deviceId = deviceId;
    this.refreshTokenHash = refreshTokenHash;
    this.familyId = familyId;
    this.status = Status.ACTIVE;
    this.ip = ip;
    this.userAgent = userAgent;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
  }

  public void rotate(Instant now) {
    this.status = Status.ROTATED;
    this.revokedAt = now;
  }

  public void revoke(Instant now) {
    this.status = Status.REVOKED;
    this.revokedAt = now;
  }

  public boolean isActive(Instant now) {
    return status == Status.ACTIVE && expiresAt.isAfter(now);
  }

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getFamilyId() {
    return familyId;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
