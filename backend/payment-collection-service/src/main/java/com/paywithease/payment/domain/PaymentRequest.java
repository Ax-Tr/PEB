package com.paywithease.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A request to collect money from a customer. Amounts are integer paise. Payment is confirmed
 * asynchronously by a signature-verified, idempotent gateway webhook; {@link #applyPayment} clamps
 * to the outstanding amount so a redelivered/over-amount webhook can never drive the balance
 * negative or past the requested total.
 */
@Entity
@Table(name = "payment_requests")
public class PaymentRequest {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "customer_id", length = 26, columnDefinition = "char(26)")
  private String customerId;

  @Column(nullable = false)
  private String reference;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "amount_paid_minor", nullable = false)
  private long amountPaidMinor;

  @Column(name = "allow_partial", nullable = false)
  private boolean allowPartial;

  private String purpose;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "invoice_id", length = 26, columnDefinition = "char(26)")
  private String invoiceId;

  private String provider;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected PaymentRequest() {}

  public PaymentRequest(
      String id,
      String tenantId,
      String customerId,
      String reference,
      long amountMinor,
      boolean allowPartial,
      String purpose,
      Instant expiresAt,
      Instant now) {
    if (amountMinor <= 0) {
      throw new IllegalArgumentException("amountMinor must be positive");
    }
    this.id = id;
    this.tenantId = tenantId;
    this.customerId = customerId;
    this.reference = reference;
    this.amountMinor = amountMinor;
    this.allowPartial = allowPartial;
    this.purpose = purpose;
    this.status = PaymentStatus.AWAITING_PAYMENT;
    this.expiresAt = expiresAt;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Outcome of applying an incoming settlement to this request. */
  public record Applied(long appliedMinor, long overpaidMinor, boolean fullyPaid) {}

  /**
   * Applies an incoming paid amount, clamped to the outstanding balance. Returns how much was
   * actually applied and any overpayment (which callers surface for reconciliation rather than
   * crediting).
   */
  public Applied applyPayment(long incomingMinor, Instant now) {
    if (incomingMinor <= 0) {
      throw new IllegalArgumentException("incoming amount must be positive");
    }
    long remaining = amountMinor - amountPaidMinor;
    if (remaining <= 0) {
      // Already settled — treat as a no-op (idempotent against duplicate settlements).
      return new Applied(0, incomingMinor, true);
    }
    long applied = Math.min(incomingMinor, remaining);
    long overpaid = incomingMinor - applied;
    amountPaidMinor += applied;
    status = (amountPaidMinor >= amountMinor) ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID;
    updatedAt = now;
    return new Applied(applied, overpaid, status == PaymentStatus.PAID);
  }

  public void markProvider(String provider) {
    this.provider = provider;
  }

  public void markFailed(Instant now) {
    if (status == PaymentStatus.AWAITING_PAYMENT || status == PaymentStatus.PARTIALLY_PAID) {
      this.status = PaymentStatus.FAILED;
      this.updatedAt = now;
    }
  }

  public void cancel(Instant now) {
    if (status == PaymentStatus.AWAITING_PAYMENT) {
      this.status = PaymentStatus.CANCELLED;
      this.updatedAt = now;
    } else {
      throw new IllegalStateException("Only an awaiting request can be cancelled");
    }
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public String getReference() {
    return reference;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public long getAmountPaidMinor() {
    return amountPaidMinor;
  }

  public boolean isAllowPartial() {
    return allowPartial;
  }

  public String getPurpose() {
    return purpose;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
