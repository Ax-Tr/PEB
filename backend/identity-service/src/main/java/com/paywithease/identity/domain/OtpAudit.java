package com.paywithease.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Append-only OTP issuance/verification history (live OTP state lives in Redis). Used for rate
 * analytics and abuse investigation; the OTP itself is never stored here.
 */
@Entity
@Table(name = "otp_requests")
public class OtpAudit {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "mobile_hash", length = 64, nullable = false)
  private String mobileHash;

  @Column(nullable = false)
  private String purpose;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected OtpAudit() {}

  public OtpAudit(String id, String mobileHash, String purpose, String status, Instant now) {
    this.id = id;
    this.mobileHash = mobileHash;
    this.purpose = purpose;
    this.status = status;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }
}
