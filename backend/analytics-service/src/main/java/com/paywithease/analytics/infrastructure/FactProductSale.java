package com.paywithease.analytics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Product/service profitability projection (one row per invoice line). Populated only when source
 * invoice events carry line-level detail; until then this table stays empty (documented follow-up).
 */
@Entity
@Table(name = "fact_product_sales")
public class FactProductSale {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "line_id", length = 26, columnDefinition = "char(26)")
  private String lineId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "invoice_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String invoiceId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "product_id", length = 26, columnDefinition = "char(26)")
  private String productId;

  @Column(name = "product_name")
  private String productName;

  @Column(name = "period_year", nullable = false)
  private int periodYear;

  @Column(name = "period_month", nullable = false)
  private int periodMonth;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(name = "revenue_minor", nullable = false)
  private long revenueMinor;

  @Column(name = "cost_minor", nullable = false)
  private long costMinor;

  protected FactProductSale() {}

  public String getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public long getRevenueMinor() {
    return revenueMinor;
  }

  public long getCostMinor() {
    return costMinor;
  }
}
