package com.paywithease.product.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.product.domain.PriceHistory;
import com.paywithease.product.domain.Product;
import com.paywithease.product.infrastructure.HsnSacRepository;
import com.paywithease.product.infrastructure.PriceHistoryRepository;
import com.paywithease.product.infrastructure.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock ProductRepository products;
  @Mock HsnSacRepository hsnSacs;
  @Mock PriceHistoryRepository priceHistory;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private ProductService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new ProductService(
            products, hsnSacs, priceHistory, audit, outbox, new ObjectMapper(), clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createValidatesAndPersists() {
    when(products.save(any(Product.class))).thenAnswer(returnsFirstArg());

    Product p =
        service.create(
            "Laptop",
            "GOOD",
            "8471",
            new BigDecimal("18"),
            "PCS",
            5000000L,
            4200000L,
            new BigDecimal("15.00"));

    assertThat(p.getTenantId()).isEqualTo("tenant1");
    assertThat(p.getName()).isEqualTo("Laptop");
    assertThat(p.getType()).isEqualTo("GOOD");
    assertThat(p.getSalePriceMinor()).isEqualTo(5000000L);
    verify(products).save(any(Product.class));
    verify(priceHistory).save(any(PriceHistory.class));
    verify(outbox).append(any(EventEnvelope.class));
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void createRejectsBadGstRate() {
    assertThatThrownBy(
            () ->
                service.create(
                    "Widget", "GOOD", "8471", new BigDecimal("7"), "PCS", 100L, 80L, null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("GST rate");
  }

  @Test
  void createRejectsUnknownType() {
    assertThatThrownBy(
            () ->
                service.create(
                    "Widget", "GADGET", "8471", new BigDecimal("18"), "PCS", 100L, 80L, null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("item type");
  }
}
