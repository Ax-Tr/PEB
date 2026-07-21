package com.paywithease.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An immutable record of a product's sale price at a point in time (paise). */
@Entity
@Table(name = "price_history")
public class PriceHistory {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "product_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String productId;

  @Column(name = "sale_price_minor", nullable = false)
  private long salePriceMinor;

  @Column(name = "effective_at", nullable = false)
  private Instant effectiveAt;

  protected PriceHistory() {}

  public PriceHistory(
      String id, String tenantId, String productId, long salePriceMinor, Instant effectiveAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.productId = productId;
    this.salePriceMinor = salePriceMinor;
    this.effectiveAt = effectiveAt;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getProductId() {
    return productId;
  }

  public long getSalePriceMinor() {
    return salePriceMinor;
  }

  public Instant getEffectiveAt() {
    return effectiveAt;
  }
}
