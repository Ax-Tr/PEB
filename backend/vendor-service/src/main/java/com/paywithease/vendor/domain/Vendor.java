package com.paywithease.vendor.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** A vendor/supplier of a tenant. Sensitive contact and tax fields are encrypted at rest. */
@Entity
@Table(name = "vendors")
public class Vendor {

  @Id
  @Column(columnDefinition = "bpchar", length = 26)
  private String id;

  @Column(name = "tenant_id", columnDefinition = "bpchar", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String name;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "mobile_enc")
  private String mobile;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "email_enc")
  private String email;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "gstin_enc")
  private String gstin;

  private String address;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Vendor() {}

  public Vendor(String id, String tenantId, String name, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.status = "ACTIVE";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void updateName(String name, Instant now) {
    if (name != null) this.name = name;
    this.updatedAt = now;
  }

  public void setMobile(String mobile) {
    this.mobile = mobile;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setGstin(String gstin) {
    this.gstin = gstin;
  }

  public void setAddress(String address) {
    this.address = address;
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

  public String getEmail() {
    return email;
  }

  public String getGstin() {
    return gstin;
  }

  public String getAddress() {
    return address;
  }

  public String getStatus() {
    return status;
  }
}
