package com.paywithease.customer.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An additional contact channel for a customer (PHONE/EMAIL/WHATSAPP). Value is encrypted. */
@Entity
@Table(name = "customer_contacts")
public class CustomerContact {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(columnDefinition = "bpchar", length = 26)
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", columnDefinition = "bpchar", length = 26, nullable = false)
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "customer_id", columnDefinition = "bpchar", length = 26, nullable = false)
  private String customerId;

  @Column(nullable = false)
  private String type;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "value_enc", nullable = false)
  private String value;

  @Column(nullable = false)
  private boolean preferred;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected CustomerContact() {}

  public CustomerContact(
      String id,
      String tenantId,
      String customerId,
      String type,
      String value,
      boolean preferred,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.customerId = customerId;
    this.type = type;
    this.value = value;
    this.preferred = preferred;
    this.createdAt = now;
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

  public String getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public boolean isPreferred() {
    return preferred;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
