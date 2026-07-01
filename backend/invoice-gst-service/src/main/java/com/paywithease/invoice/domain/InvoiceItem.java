package com.paywithease.invoice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** A single line on an invoice with its computed GST split. Amounts are integer paise. */
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "invoice_id", length = 26, nullable = false)
  private String invoiceId;

  @Column(name = "product_id", length = 26)
  private String productId;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "hsn_sac")
  private String hsnSac;

  @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
  private BigDecimal quantity;

  @Column(name = "unit_price_minor", nullable = false)
  private long unitPriceMinor;

  @Column(name = "discount_minor", nullable = false)
  private long discountMinor;

  @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal gstRate;

  @Column(name = "taxable_value_minor", nullable = false)
  private long taxableValueMinor;

  @Column(name = "cgst_minor", nullable = false)
  private long cgstMinor;

  @Column(name = "sgst_minor", nullable = false)
  private long sgstMinor;

  @Column(name = "igst_minor", nullable = false)
  private long igstMinor;

  @Column(name = "line_total_minor", nullable = false)
  private long lineTotalMinor;

  protected InvoiceItem() {}

  public InvoiceItem(
      String id,
      String tenantId,
      String invoiceId,
      String productId,
      String description,
      String hsnSac,
      BigDecimal quantity,
      long unitPriceMinor,
      long discountMinor,
      BigDecimal gstRate,
      long taxableValueMinor,
      long cgstMinor,
      long sgstMinor,
      long igstMinor,
      long lineTotalMinor) {
    this.id = id;
    this.tenantId = tenantId;
    this.invoiceId = invoiceId;
    this.productId = productId;
    this.description = description;
    this.hsnSac = hsnSac;
    this.quantity = quantity;
    this.unitPriceMinor = unitPriceMinor;
    this.discountMinor = discountMinor;
    this.gstRate = gstRate;
    this.taxableValueMinor = taxableValueMinor;
    this.cgstMinor = cgstMinor;
    this.sgstMinor = sgstMinor;
    this.igstMinor = igstMinor;
    this.lineTotalMinor = lineTotalMinor;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public String getProductId() {
    return productId;
  }

  public String getDescription() {
    return description;
  }

  public String getHsnSac() {
    return hsnSac;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public long getUnitPriceMinor() {
    return unitPriceMinor;
  }

  public long getDiscountMinor() {
    return discountMinor;
  }

  public BigDecimal getGstRate() {
    return gstRate;
  }

  public long getTaxableValueMinor() {
    return taxableValueMinor;
  }

  public long getCgstMinor() {
    return cgstMinor;
  }

  public long getSgstMinor() {
    return sgstMinor;
  }

  public long getIgstMinor() {
    return igstMinor;
  }

  public long getLineTotalMinor() {
    return lineTotalMinor;
  }
}
