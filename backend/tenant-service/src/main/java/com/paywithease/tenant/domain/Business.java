package com.paywithease.tenant.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** A business/tenant. {@code id} is the canonical tenant_id used across all services. */
@Entity
@Table(name = "businesses")
public class Business {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "owner_user_id", length = 26, nullable = false)
  private String ownerUserId;

  @Column(name = "legal_name", nullable = false)
  private String legalName;

  @Column(name = "trade_name")
  private String tradeName;

  @Column(name = "business_type", nullable = false)
  private String businessType;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "gstin_enc")
  private String gstin;

  @Column(name = "gstin_hash", length = 64)
  private String gstinHash;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "pan_enc")
  private String pan;

  private String udyam;

  @Column(name = "state_code", length = 2, nullable = false)
  private String stateCode;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Business() {}

  public Business(
      String id,
      String ownerUserId,
      String legalName,
      String businessType,
      String stateCode,
      Instant now) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.legalName = legalName;
    this.businessType = businessType;
    this.stateCode = stateCode;
    this.status = "ACTIVE";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void updateProfile(String legalName, String tradeName, String businessType, Instant now) {
    if (legalName != null) this.legalName = legalName;
    this.tradeName = tradeName;
    if (businessType != null) this.businessType = businessType;
    this.updatedAt = now;
  }

  public void setTaxIdentifiers(
      String gstin, String gstinHash, String pan, String udyam, Instant now) {
    this.gstin = gstin;
    this.gstinHash = gstinHash;
    this.pan = pan;
    this.udyam = udyam;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public String getLegalName() {
    return legalName;
  }

  public String getTradeName() {
    return tradeName;
  }

  public String getBusinessType() {
    return businessType;
  }

  public String getGstin() {
    return gstin;
  }

  public String getGstinHash() {
    return gstinHash;
  }

  public String getPan() {
    return pan;
  }

  public String getUdyam() {
    return udyam;
  }

  public String getStateCode() {
    return stateCode;
  }

  public String getStatus() {
    return status;
  }
}
