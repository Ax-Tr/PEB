package com.paywithease.customer.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.customer.domain.Customer;
import com.paywithease.customer.domain.MobileNumber;
import com.paywithease.customer.infrastructure.CustomerContactRepository;
import com.paywithease.customer.infrastructure.CustomerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer directory. Validates mobile numbers, enforces per-tenant mobile uniqueness via a blind
 * index, emits domain events, and writes audit. Ledger summary is a Sprint-2 stub.
 */
@Service
public class CustomerService {

  private final CustomerRepository customers;
  private final CustomerContactRepository contacts;
  private final BlindIndex blindIndex;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public CustomerService(
      CustomerRepository customers,
      CustomerContactRepository contacts,
      BlindIndex blindIndex,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.customers = customers;
    this.contacts = contacts;
    this.blindIndex = blindIndex;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Sprint-2 stub: real figures arrive once invoice/ledger services exist. */
  public record LedgerSummary(
      String customerId, long totalReceivableMinor, long totalReceivedMinor, int openInvoices) {}

  @Transactional
  public Customer create(
      String name, String rawMobile, String email, String address, String gstin) {
    String tenantId = TenantContext.requireTenantId();
    if (!MobileNumber.isValid(rawMobile)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid Indian mobile number");
    }
    String normalizedMobile = MobileNumber.of(rawMobile).value();
    String mobileHash = blindIndex.hash(normalizedMobile);
    if (customers.existsByTenantIdAndMobileHash(tenantId, mobileHash)) {
      throw new ApiException(ErrorCode.CONFLICT, "Customer with this mobile already exists");
    }

    Instant now = clock.instant();
    String id = Ulid.newId();
    Customer customer = new Customer(id, tenantId, name, normalizedMobile, mobileHash, now);
    customer.setEmail(email);
    customer.setAddress(address);
    customer.setGstin(gstin);
    customers.save(customer);

    audit.record("CUSTOMER_CREATED", "customer", id, Map.of("name", name));
    emit("CUSTOMER_CREATED", id, Map.of("name", name));
    return customer;
  }

  @Transactional(readOnly = true)
  public Optional<Customer> searchByMobile(String rawMobile) {
    String tenantId = TenantContext.requireTenantId();
    if (!MobileNumber.isValid(rawMobile)) {
      return Optional.empty();
    }
    String mobileHash = blindIndex.hash(MobileNumber.of(rawMobile).value());
    return customers.findByTenantIdAndMobileHash(tenantId, mobileHash);
  }

  @Transactional(readOnly = true)
  public Customer get(String id) {
    String tenantId = TenantContext.requireTenantId();
    Customer customer = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
    if (!customer.getTenantId().equals(tenantId)) {
      throw ApiException.notFound("Customer");
    }
    return customer;
  }

  @Transactional(readOnly = true)
  public List<Customer> list() {
    return customers.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  @Transactional(readOnly = true)
  public LedgerSummary ledgerSummary(String id) {
    Customer customer = get(id);
    // Sprint-2 stub: zeroed until invoice/ledger services publish per-customer balances.
    return new LedgerSummary(customer.getId(), 0L, 0L, 0);
  }

  private void emit(String eventType, String aggregateId, Map<String, ?> data) {
    String tenantId = TenantContext.requireTenantId();
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService("customer-service")
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(objectMapper.valueToTree(data))
            .build(clock.instant());
    outbox.append(envelope);
  }
}
