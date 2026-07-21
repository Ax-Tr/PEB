package com.paywithease.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only inbound webhook record. Unique on {@code (provider, providerEventId)} so a
 * redelivered webhook is detected and skipped. The raw payload is retained as audit/reconciliation
 * evidence.
 */
@Entity
@Table(name = "payment_webhooks")
public class PaymentWebhook {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26)
  private String tenantId;

  @Column(nullable = false)
  private String provider;

  @Column(name = "provider_event_id", nullable = false)
  private String providerEventId;

  @Column(name = "signature_verified", nullable = false)
  private boolean signatureVerified;

  @Column(nullable = false)
  private String status;

  private String reference;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
  private String rawPayload;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected PaymentWebhook() {}

  public PaymentWebhook(
      String id,
      String tenantId,
      String provider,
      String providerEventId,
      boolean signatureVerified,
      String status,
      String reference,
      String rawPayload,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.provider = provider;
    this.providerEventId = providerEventId;
    this.signatureVerified = signatureVerified;
    this.status = status;
    this.reference = reference;
    this.rawPayload = rawPayload;
    this.receivedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderEventId() {
    return providerEventId;
  }
}
