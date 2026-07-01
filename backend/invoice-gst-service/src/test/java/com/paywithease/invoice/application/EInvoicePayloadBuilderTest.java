package com.paywithease.invoice.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.invoice.domain.DocumentType;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.domain.InvoiceItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EInvoicePayloadBuilderTest {

  private EInvoicePayloadBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new EInvoicePayloadBuilder(new ObjectMapper());
  }

  private static Invoice b2bInvoice(String customerGstin) {
    return new Invoice(
        "inv1",
        "tenant1",
        DocumentType.TAX_INVOICE.name(),
        "B2B",
        "cust1",
        "Acme",
        customerGstin,
        "29",
        "27",
        "INV/2026-27/00001",
        "2026-27",
        LocalDate.of(2026, 5, 15),
        null,
        null,
        false,
        true,
        100000,
        0,
        0,
        18000,
        18000,
        118000,
        Instant.parse("2026-05-15T10:00:00Z"));
  }

  private static InvoiceItem item(String hsn) {
    return new InvoiceItem(
        "item1",
        "tenant1",
        "inv1",
        "prod1",
        "Widget",
        hsn,
        BigDecimal.ONE,
        100000,
        0,
        new BigDecimal("18"),
        100000,
        0,
        0,
        18000,
        118000);
  }

  @Test
  void b2bMissingBuyerGstinIsNotReady() {
    Readiness r = builder.build(b2bInvoice(null), List.of(item("998314")));
    assertThat(r.ready()).isFalse();
    assertThat(r.missingFields()).contains("buyerGstin");
  }

  @Test
  void completeB2bIsReady() {
    Readiness r = builder.build(b2bInvoice("29ABCDE1234F1Z5"), List.of(item("998314")));
    assertThat(r.ready()).isTrue();
    assertThat(r.missingFields()).isEmpty();
  }
}
