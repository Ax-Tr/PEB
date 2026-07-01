package com.paywithease.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A row in the HSN/SAC master reference table. HSN classifies goods, SAC classifies services; both
 * carry the default GST rate used to prefill a product's tax rate at catalog-entry time.
 */
@Entity
@Table(name = "hsn_sac_master")
public class HsnSac {

  @Id
  @Column(length = 10)
  private String code;

  @Column(nullable = false)
  private String description;

  @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
  private BigDecimal gstRate;

  @Column(nullable = false)
  private String kind; // HSN | SAC

  protected HsnSac() {}

  public HsnSac(String code, String description, BigDecimal gstRate, String kind) {
    this.code = code;
    this.description = description;
    this.gstRate = gstRate;
    this.kind = kind;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getGstRate() {
    return gstRate;
  }

  public String getKind() {
    return kind;
  }
}
