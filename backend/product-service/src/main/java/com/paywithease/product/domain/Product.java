package com.paywithease.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A tenant's catalog line: a product (GOOD) or a service (SERVICE) with GST and pricing. */
@Entity
@Table(name = "products")
public class Product {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(nullable = false)
  private String name;

  @Column(name = "type", nullable = false)
  private String type; // GOOD | SERVICE

  @Column(name = "hsn_sac", nullable = false)
  private String hsnSac;

  @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
  private BigDecimal gstRate;

  @Column(nullable = false)
  private String unit; // e.g. PCS, HOUR

  @Column(name = "sale_price_minor", nullable = false)
  private long salePriceMinor;

  @Column(name = "purchase_price_minor", nullable = false)
  private long purchasePriceMinor;

  @Column(name = "margin_default", precision = 5, scale = 2)
  private BigDecimal marginDefault;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Product() {}

  public Product(
      String id,
      String tenantId,
      String name,
      String type,
      String hsnSac,
      BigDecimal gstRate,
      String unit,
      long salePriceMinor,
      long purchasePriceMinor,
      BigDecimal marginDefault,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.name = name;
    this.type = type;
    this.hsnSac = hsnSac;
    this.gstRate = gstRate;
    this.unit = unit;
    this.salePriceMinor = salePriceMinor;
    this.purchasePriceMinor = purchasePriceMinor;
    this.marginDefault = marginDefault;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void update(
      String name,
      String type,
      String hsnSac,
      BigDecimal gstRate,
      String unit,
      long salePriceMinor,
      long purchasePriceMinor,
      BigDecimal marginDefault,
      Instant now) {
    if (name != null) this.name = name;
    if (type != null) this.type = type;
    if (hsnSac != null) this.hsnSac = hsnSac;
    if (gstRate != null) this.gstRate = gstRate;
    if (unit != null) this.unit = unit;
    this.salePriceMinor = salePriceMinor;
    this.purchasePriceMinor = purchasePriceMinor;
    this.marginDefault = marginDefault;
    this.updatedAt = now;
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

  public String getType() {
    return type;
  }

  public String getHsnSac() {
    return hsnSac;
  }

  public BigDecimal getGstRate() {
    return gstRate;
  }

  public String getUnit() {
    return unit;
  }

  public long getSalePriceMinor() {
    return salePriceMinor;
  }

  public long getPurchasePriceMinor() {
    return purchasePriceMinor;
  }

  public BigDecimal getMarginDefault() {
    return marginDefault;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
