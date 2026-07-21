package com.paywithease.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** GST/TDS posture for a business. */
@Entity
@Table(name = "business_tax_profiles")
public class BusinessTaxProfile {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "gst_registered", nullable = false)
  private boolean gstRegistered;

  @Column(name = "composition_scheme", nullable = false)
  private boolean compositionScheme;

  @Column(name = "reverse_charge_enabled", nullable = false)
  private boolean reverseChargeEnabled;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "default_place_of_supply", columnDefinition = "char(2)")
  private String defaultPlaceOfSupply;

  @Column(name = "tds_applicable", nullable = false)
  private boolean tdsApplicable;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected BusinessTaxProfile() {}

  public BusinessTaxProfile(String tenantId, String defaultPlaceOfSupply, Instant now) {
    this.tenantId = tenantId;
    this.defaultPlaceOfSupply = defaultPlaceOfSupply;
    this.updatedAt = now;
  }

  public void update(
      boolean gstRegistered,
      boolean compositionScheme,
      boolean reverseChargeEnabled,
      String defaultPlaceOfSupply,
      boolean tdsApplicable,
      Instant now) {
    this.gstRegistered = gstRegistered;
    this.compositionScheme = compositionScheme;
    this.reverseChargeEnabled = reverseChargeEnabled;
    this.defaultPlaceOfSupply = defaultPlaceOfSupply;
    this.tdsApplicable = tdsApplicable;
    this.updatedAt = now;
  }

  public String getTenantId() {
    return tenantId;
  }

  public boolean isGstRegistered() {
    return gstRegistered;
  }

  public boolean isCompositionScheme() {
    return compositionScheme;
  }

  public boolean isReverseChargeEnabled() {
    return reverseChargeEnabled;
  }

  public String getDefaultPlaceOfSupply() {
    return defaultPlaceOfSupply;
  }

  public boolean isTdsApplicable() {
    return tdsApplicable;
  }
}
