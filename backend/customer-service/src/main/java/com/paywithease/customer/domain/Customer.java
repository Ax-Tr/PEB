package com.paywithease.customer.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** A customer of a business. Tenant-scoped by {@code tenantId}; mobile is unique per tenant. */
@Entity
@Table(name = "customers")
public class Customer {

  @Id
  @Column(columnDefinition = "bpchar", length = 26)
  private String id;

  @Column(name = "tenant_id", columnDefinition = "bpchar", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String name;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "mobile_enc", nullable = false)
  private String mobile;

  @Column(name = "mobile_hash", columnDefinition = "bpchar", length = 64, nullable = false)
  private String mobileHash;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "email_enc")
  private String email;

  private String address;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "gstin_enc")
  private String gstin;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Customer() {}

  public Customer(
      String id, String tenantId, String name, String mobile, String mobileHash, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.mobile = mobile;
    this.mobileHash = mobileHash;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void updateName(String name, Instant now) {
    if (name != null) this.name = name;
    this.updatedAt = now;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setGstin(String gstin) {
    this.gstin = gstin;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public String getMobile() {
    return mobile;
  }

  public String getMobileHash() {
    return mobileHash;
  }

  public String getEmail() {
    return email;
  }

  public String getAddress() {
    return address;
  }

  public String getGstin() {
    return gstin;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
