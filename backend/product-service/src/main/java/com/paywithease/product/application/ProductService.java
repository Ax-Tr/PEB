package com.paywithease.product.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.product.domain.GstRate;
import com.paywithease.product.domain.HsnSac;
import com.paywithease.product.domain.ItemType;
import com.paywithease.product.domain.PriceHistory;
import com.paywithease.product.domain.Product;
import com.paywithease.product.infrastructure.HsnSacRepository;
import com.paywithease.product.infrastructure.PriceHistoryRepository;
import com.paywithease.product.infrastructure.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Product/service catalog. Validates item type & GST rate, seeds price history, emits events. */
@Service
public class ProductService {

  private final ProductRepository products;
  private final HsnSacRepository hsnSacs;
  private final PriceHistoryRepository priceHistory;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ProductService(
      ProductRepository products,
      HsnSacRepository hsnSacs,
      PriceHistoryRepository priceHistory,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.products = products;
    this.hsnSacs = hsnSacs;
    this.priceHistory = priceHistory;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public Product create(
      String name,
      String type,
      String hsnSac,
      BigDecimal gstRatePercent,
      String unit,
      long salePriceMinor,
      long purchasePriceMinor,
      BigDecimal marginDefault) {
    if (!ItemType.isValid(type)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown item type: " + type);
    }
    if (!GstRate.isAllowed(gstRatePercent)) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "unsupported GST rate: " + gstRatePercent);
    }
    if (hsnSac == null || hsnSac.isBlank()) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          ItemType.SERVICE.name().equals(type) ? "SAC is required" : "HSN is required");
    }

    String tenantId = TenantContext.requireTenantId();
    Instant now = clock.instant();
    String id = Ulid.newId();
    Product product =
        new Product(
            id,
            tenantId,
            name,
            type,
            hsnSac.trim(),
            gstRatePercent,
            unit,
            salePriceMinor,
            purchasePriceMinor,
            marginDefault,
            now);
    products.save(product);
    priceHistory.save(new PriceHistory(Ulid.newId(), tenantId, id, salePriceMinor, now));

    audit.record("PRODUCT_CREATED", "product", id, Map.of("name", name, "type", type));
    emit(
        "PRODUCT_CREATED",
        tenantId,
        id,
        Map.of("name", name, "type", type, "hsnSac", hsnSac.trim()));
    return product;
  }

  @Transactional(readOnly = true)
  public List<Product> list() {
    return products.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  @Transactional(readOnly = true)
  public Product get(String id) {
    Product product = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
    if (!product.getTenantId().equals(TenantContext.requireTenantId())) {
      throw ApiException.notFound("Product");
    }
    return product;
  }

  @Transactional(readOnly = true)
  public List<HsnSac> searchHsnSac(String q) {
    if (q == null || q.isBlank()) {
      return List.of();
    }
    return hsnSacs.search(q.trim(), Limit.of(20));
  }

  private void emit(String eventType, String tenantId, String aggregateId, Map<String, ?> data) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService("product-service")
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(objectMapper.valueToTree(data))
            .build(clock.instant());
    outbox.append(envelope);
  }
}
