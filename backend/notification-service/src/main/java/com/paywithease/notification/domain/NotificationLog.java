package com.paywithease.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A record of one outbound notification and its delivery lifecycle. */
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String channel;

  @Column(nullable = false)
  private String recipient;

  @Column(name = "template_code")
  private String templateCode;

  private String subject;

  @Column(nullable = false)
  private String body;

  @Column(nullable = false)
  private String status; // QUEUED, SENT, DELIVERED, FAILED

  private String provider;

  @Column(name = "provider_ref")
  private String providerRef;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "reminder_id", length = 26)
  private String reminderId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected NotificationLog() {}

  public NotificationLog(
      String id,
      String tenantId,
      Channel channel,
      String recipient,
      String templateCode,
      String subject,
      String body,
      String reminderId,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.channel = channel.name();
    this.recipient = recipient;
    this.templateCode = templateCode;
    this.subject = subject;
    this.body = body;
    this.reminderId = reminderId;
    this.status = "QUEUED";
    this.attempts = 0;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void recordAttempt() {
    this.attempts++;
  }

  public void markSent(String provider, String providerRef, Instant now) {
    this.status = "SENT";
    this.provider = provider;
    this.providerRef = providerRef;
    this.failureReason = null;
    this.updatedAt = now;
  }

  public void markFailed(String provider, String reason, Instant now) {
    this.status = "FAILED";
    this.provider = provider;
    this.failureReason = reason;
    this.updatedAt = now;
  }

  public void markDelivered(Instant now) {
    this.status = "DELIVERED";
    this.updatedAt = now;
  }

  public void markDeliveryFailed(String reason, Instant now) {
    this.status = "FAILED";
    this.failureReason = reason;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getChannel() {
    return channel;
  }

  public String getStatus() {
    return status;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderRef() {
    return providerRef;
  }

  public int getAttempts() {
    return attempts;
  }
}
