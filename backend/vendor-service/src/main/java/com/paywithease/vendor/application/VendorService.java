package com.paywithease.vendor.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.vendor.domain.BankAccountSource;
import com.paywithease.vendor.domain.BankAccountStatus;
import com.paywithease.vendor.domain.Vendor;
import com.paywithease.vendor.domain.VendorBankAccount;
import com.paywithease.vendor.infrastructure.VendorBankAccountRepository;
import com.paywithease.vendor.infrastructure.VendorRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vendor onboarding and bank-account capture. Bank accounts are saved {@code PENDING_REVIEW} and
 * become usable only once a user confirms them; {@code VENDOR_BANK_DETAILS_CHANGED} is emitted on
 * confirmation, never on capture (product rule #7 — OCR/entered details must be user-reviewed).
 */
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
    get(vendorId); // ensures vendor exists + belongs to tenant

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
    // Intentionally NOT emitting VENDOR_BANK_DETAILS_CHANGED: the account is not yet usable and
    // must be user-reviewed (product rule #7) before downstream services can rely on it.
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
    // No event: rejected details never become usable.
    return account;
  }

  @Transactional(readOnly = true)
  public List<VendorBankAccount> listBankAccounts(String vendorId) {
    get(vendorId); // ensures vendor exists + belongs to tenant
    return bankAccounts.findByVendorId(vendorId);
  }

  /** Loads a bank account and asserts it belongs to the vendor + tenant and is still pending. */
  private VendorBankAccount requirePendingReview(
      String vendorId, String bankAccountId, String tenantId) {
    VendorBankAccount account =
        bankAccounts
            .findById(bankAccountId)
            .orElseThrow(() -> ApiException.notFound("Bank account"));
    if (!account.getTenantId().equals(tenantId) || !account.getVendorId().equals(vendorId)) {
      throw ApiException.notFound("Bank account");
    }
    if (account.getStatus() != BankAccountStatus.PENDING_REVIEW) {
      throw new ApiException(ErrorCode.CONFLICT, "not pending review");
    }
    return account;
  }

  private static BankAccountSource parseSource(String source) {
    try {
      return BankAccountSource.valueOf(source == null ? "" : source.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown bank account source: " + source);
    }
  }

  private static void requireTenant(String tenantId) {
    if (!TenantContext.requireTenantId().equals(tenantId)) {
      throw ApiException.notFound("Vendor");
    }
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
            .sourceService("vendor-service")
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(objectMapper.valueToTree(data))
            .build(clock.instant());
    outbox.append(envelope);
  }
}
