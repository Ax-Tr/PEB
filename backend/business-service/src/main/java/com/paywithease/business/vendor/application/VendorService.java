package com.paywithease.business.vendor.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.business.vendor.domain.*;
import com.paywithease.business.vendor.infrastructure.VendorBankAccountRepository;
import com.paywithease.business.vendor.infrastructure.VendorRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

  private final VendorRepository vendors;
  private final VendorBankAccountRepository bankAccounts;
  private final BlindIndex blindIndex;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public VendorService(
      VendorRepository vendors,
      VendorBankAccountRepository bankAccounts,
      BlindIndex blindIndex,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.vendors = vendors;
    this.bankAccounts = bankAccounts;
    this.blindIndex = blindIndex;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public Vendor create(String name, String mobile, String email, String gstin, String address) {
    String tenantId = TenantContext.requireTenantId();
    Instant now = clock.instant();
    String id = Ulid.newId();
    Vendor vendor = new Vendor(id, tenantId, name, now);
    if (isPresent(mobile)) vendor.setMobile(mobile.trim());
    if (isPresent(email)) vendor.setEmail(email.trim());
    if (isPresent(gstin)) vendor.setGstin(gstin.trim());
    if (isPresent(address)) vendor.setAddress(address);
    vendors.save(vendor);
    audit.record("VENDOR_CREATED", "vendor", id, Map.of("name", name));
    emit("VENDOR_CREATED", tenantId, id, Map.of("name", name));
    return vendor;
  }

  @Transactional(readOnly = true)
  public Vendor get(String id) {
    Vendor vendor = vendors.findById(id).orElseThrow(() -> ApiException.notFound("Vendor"));
    requireTenant(vendor.getTenantId());
    return vendor;
  }

  @Transactional(readOnly = true)
  public List<Vendor> list() {
    return vendors.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  @Transactional
  public VendorBankAccount addBankAccount(
      String vendorId,
      String accountNumber,
      String ifsc,
      String upi,
      String bankName,
      String holderName,
      String source) {
    String tenantId = TenantContext.requireTenantId();
    get(vendorId);
    String accountNumberHash = blindIndex.hash(accountNumber);
    if (bankAccounts.existsByVendorIdAndAccountNumberHash(vendorId, accountNumberHash)) {
      throw new ApiException(ErrorCode.CONFLICT, "bank account already exists for vendor");
    }
    BankAccountSource parsedSource = parseSource(source);
    Instant now = clock.instant();
    String baId = Ulid.newId();
    VendorBankAccount account =
        new VendorBankAccount(
            baId,
            tenantId,
            vendorId,
            accountNumber,
            accountNumberHash,
            ifsc,
            isPresent(upi) ? upi.trim() : null,
            bankName,
            holderName,
            parsedSource,
            now);
    bankAccounts.save(account);
    audit.record(
        "VENDOR_BANK_ACCOUNT_ADDED",
        "vendor_bank_account",
        baId,
        Map.of("source", parsedSource.name(), "status", BankAccountStatus.PENDING_REVIEW.name()));
    return account;
  }

  @Transactional
  public VendorBankAccount confirmBankAccount(
      String vendorId, String bankAccountId, String reviewerActorId) {
    String tenantId = TenantContext.requireTenantId();
    VendorBankAccount account = requirePendingReview(vendorId, bankAccountId, tenantId);
    account.confirm(reviewerActorId, clock.instant());
    bankAccounts.save(account);
    audit.record(
        "VENDOR_BANK_ACCOUNT_VERIFIED",
        "vendor_bank_account",
        bankAccountId,
        Map.of("vendorId", vendorId, "reviewedBy", nullSafe(reviewerActorId)));
    emit(
        "VENDOR_BANK_DETAILS_CHANGED",
        tenantId,
        vendorId,
        Map.of("vendorId", vendorId, "bankAccountId", bankAccountId));
    return account;
  }

  @Transactional
  public VendorBankAccount rejectBankAccount(
      String vendorId, String bankAccountId, String reviewerActorId) {
    String tenantId = TenantContext.requireTenantId();
    VendorBankAccount account = requirePendingReview(vendorId, bankAccountId, tenantId);
    account.reject(reviewerActorId, clock.instant());
    bankAccounts.save(account);
    audit.record(
        "VENDOR_BANK_ACCOUNT_REJECTED",
        "vendor_bank_account",
        bankAccountId,
        Map.of("vendorId", vendorId, "reviewedBy", nullSafe(reviewerActorId)));
    return account;
  }

  @Transactional(readOnly = true)
  public List<VendorBankAccount> listBankAccounts(String vendorId) {
    get(vendorId);
    return bankAccounts.findByVendorId(vendorId);
  }

  private VendorBankAccount requirePendingReview(
      String vendorId, String bankAccountId, String tenantId) {
    VendorBankAccount account =
        bankAccounts
            .findById(bankAccountId)
            .orElseThrow(() -> ApiException.notFound("Bank account"));
    if (!account.getTenantId().equals(tenantId) || !account.getVendorId().equals(vendorId))
      throw ApiException.notFound("Bank account");
    if (account.getStatus() != BankAccountStatus.PENDING_REVIEW)
      throw new ApiException(ErrorCode.CONFLICT, "not pending review");
    return account;
  }

  private static BankAccountSource parseSource(String source) {
    try {
      return BankAccountSource.valueOf(source == null ? "" : source.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown source: " + source);
    }
  }

  private static void requireTenant(String tenantId) {
    if (!TenantContext.requireTenantId().equals(tenantId)) throw ApiException.notFound("Vendor");
  }

  private static boolean isPresent(String s) {
    return s != null && !s.isBlank();
  }

  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }

  private void emit(String eventType, String tenantId, String aggregateId, Map<String, ?> data) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService("business-service")
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(objectMapper.valueToTree(data))
            .build(clock.instant());
    outbox.append(envelope);
  }
}
