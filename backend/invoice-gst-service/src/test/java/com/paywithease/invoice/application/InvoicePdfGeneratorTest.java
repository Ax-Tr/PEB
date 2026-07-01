package com.paywithease.invoice.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywithease.invoice.domain.DocumentType;
import com.paywithease.invoice.domain.GstTaxLine;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.domain.InvoiceItem;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoicePdfGeneratorTest {

  private final InvoicePdfGenerator generator = new InvoicePdfGenerator();

  @Test
  void generatesPdfBytes() {
    Invoice inv =
        new Invoice(
            "inv1",
            "tenant1",
            DocumentType.TAX_INVOICE.name(),
            "B2B",
            "cust1",
            "Acme",
            "29ABCDE1234F1Z5",
            "27",
            "27",
            "INV/2026-27/00001",
            "2026-27",
            LocalDate.of(2026, 5, 15),
            null,
            null,
            false,
            true,
            100000,
            9000,
            9000,
            0,
            18000,
            118000,
            Instant.parse("2026-05-15T10:00:00Z"));
    InvoiceItem item =
        new InvoiceItem(
            "item1",
            "tenant1",
            "inv1",
            "prod1",
            "Widget",
            "998314",
            BigDecimal.ONE,
            100000,
            0,
            new BigDecimal("18"),
            100000,
            9000,
            9000,
            0,
            118000);
    GstTaxLine taxLine =
        new GstTaxLine("tl1", "tenant1", "inv1", new BigDecimal("18"), 100000, 9000, 9000, 0);

    byte[] bytes = generator.generate(inv, List.of(item), List.of(taxLine));

    assertThat(bytes).isNotEmpty();
    String header = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
    assertThat(header).isEqualTo("%PDF");
  }
}
