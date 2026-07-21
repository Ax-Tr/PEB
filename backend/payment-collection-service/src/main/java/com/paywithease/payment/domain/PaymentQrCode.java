package com.paywithease.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** The UPI intent URI (QR content) and hosted payment link generated for a payment request. */
@Entity
@Table(name = "payment_qr_codes")
public class PaymentQrCode {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "payment_request_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String paymentRequestId;

  @Column(name = "upi_uri", nullable = false)
  private String upiUri;

  @Column(name = "payment_link", nullable = false)
  private String paymentLink;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected PaymentQrCode() {}

  public PaymentQrCode(
      String id,
      String tenantId,
      String paymentRequestId,
      String upiUri,
      String paymentLink,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.paymentRequestId = paymentRequestId;
    this.upiUri = upiUri;
    this.paymentLink = paymentLink;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }

  public String getUpiUri() {
    return upiUri;
  }

  public String getPaymentLink() {
    return paymentLink;
  }
}
