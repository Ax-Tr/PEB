package com.paywithease.tenant.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A branch/location of a business. */
@Entity
@Table(name = "branches")
public class Branch {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String name;

  @Column(name = "state_code", length = 2, nullable = false)
  private String stateCode;

  private String address;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "gstin_enc")
  private String gstin;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Branch() {}

  public Branch(
      String id, String tenantId, String name, String stateCode, String address, Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.stateCode = stateCode;
    this.address = address;
    this.createdAt = now;
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

  public String getStateCode() {
    return stateCode;
  }

  public String getAddress() {
    return address;
  }
}
