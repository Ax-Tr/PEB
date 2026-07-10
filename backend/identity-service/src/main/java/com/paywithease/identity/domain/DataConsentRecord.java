package com.paywithease.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** DPDP consent ledger entry: who consented to what purpose/notice-version and when. */
@Entity
@Table(name = "data_consent_records")
public class DataConsentRecord {

  @Id
  @Column(columnDefinition = "char(26)")
  private String id;

  @Column(name = "user_id", columnDefinition = "char(26)", nullable = false)
  private String userId;

  @Column(nullable = false)
  private String purpose;

  @Column(nullable = false)
  private String version;

  @Column(nullable = false)
  private boolean granted;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected DataConsentRecord() {}

  public DataConsentRecord(
      String id, String userId, String purpose, String version, boolean granted, Instant now) {
    this.id = id;
    this.userId = userId;
    this.purpose = purpose;
    this.version = version;
    this.granted = granted;
    this.occurredAt = now;
  }

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getPurpose() {
    return purpose;
  }
}
