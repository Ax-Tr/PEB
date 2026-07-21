package com.paywithease.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.identity.domain.tenant.Branch;
import com.paywithease.identity.domain.tenant.Business;
import com.paywithease.identity.domain.tenant.BusinessSettings;
import com.paywithease.identity.domain.tenant.BusinessTaxProfile;
import com.paywithease.identity.domain.tenant.BusinessType;
import com.paywithease.identity.domain.tenant.Gstin;
import com.paywithease.identity.domain.tenant.Pan;
import com.paywithease.identity.domain.tenant.Udyam;
import com.paywithease.identity.infrastructure.tenant.BranchRepository;
import com.paywithease.identity.infrastructure.tenant.BusinessRepository;
import com.paywithease.identity.infrastructure.tenant.SettingsRepository;
import com.paywithease.identity.infrastructure.tenant.TaxProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business onboarding & settings. Validates GSTIN/PAN/Udyam, emits domain events, writes audit. */
@Service
public class BusinessService {

  private final BusinessRepository businesses;
  private final BranchRepository branches;
  private final TaxProfileRepository taxProfiles;
  private final SettingsRepository settings;
  private final BlindIndex blindIndex;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public BusinessService(
      BusinessRepository businesses,
      BranchRepository branches,
      TaxProfileRepository taxProfiles,
      SettingsRepository settings,
      BlindIndex blindIndex,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.businesses = businesses;
    this.branches = branches;
    this.taxProfiles = taxProfiles;
    this.settings = settings;
    this.blindIndex = blindIndex;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public Business create(
      String ownerUserId,
      String legalName,
      String tradeName,
      String businessType,
      String stateCode) {
    if (!BusinessType.isValid(businessType)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown businessType: " + businessType);
    }
    Instant now = clock.instant();
    String id = Ulid.newId();
    Business business = new Business(id, ownerUserId, legalName, businessType, stateCode, now);
    business.updateProfile(legalName, tradeName, businessType, now);
    businesses.save(business);
    taxProfiles.save(new BusinessTaxProfile(id, stateCode, now));
    settings.save(new BusinessSettings(id, now));

    enrichContext(id, ownerUserId);
    emit(
        "BUSINESS_CREATED",
        id,
        ownerUserId,
        Map.of("legalName", legalName, "businessType", businessType));
    audit.record("BUSINESS_CREATED", "business", id, Map.of("legalName", legalName));
    return business;
  }

  @Transactional(readOnly = true)
  public Business get(String id) {
    return businesses.findById(id).orElseThrow(() -> ApiException.notFound("Business"));
  }

  @Transactional
  public Business updateProfile(
      String id, String legalName, String tradeName, String businessType) {
    if (businessType != null && !BusinessType.isValid(businessType)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown businessType: " + businessType);
    }
    Business business = get(id);
    business.updateProfile(legalName, tradeName, businessType, clock.instant());
    audit.record("BUSINESS_PROFILE_UPDATED", "business", id, Map.of());
    return business;
  }

  @Transactional
  public Business setTaxIdentifiers(String id, String gstin, String pan, String udyam) {
    Business business = get(id);
    Instant now = clock.instant();

    String gstinHash = null;
    String normalizedGstin = null;
    if (isPresent(gstin)) {
      if (!Gstin.isValid(gstin)) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid GSTIN");
      }
      Gstin g = Gstin.of(gstin);
      normalizedGstin = g.value();
      gstinHash = blindIndex.hash(normalizedGstin);
      if (!gstinHash.equals(business.getGstinHash()) && businesses.existsByGstinHash(gstinHash)) {
        throw new ApiException(ErrorCode.CONFLICT, "GSTIN already registered to another business");
      }
    }
    String normalizedPan = null;
    if (isPresent(pan)) {
      if (!Pan.isValid(pan)) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid PAN");
      }
      normalizedPan = Pan.of(pan).value();
    }
    String normalizedUdyam = null;
    if (isPresent(udyam)) {
      if (!Udyam.isValid(udyam)) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid Udyam number");
      }
      normalizedUdyam = Udyam.of(udyam).value();
    }

    business.setTaxIdentifiers(normalizedGstin, gstinHash, normalizedPan, normalizedUdyam, now);
    audit.record(
        "TAX_IDENTIFIERS_UPDATED",
        "business",
        id,
        Map.of("gstinPresent", normalizedGstin != null, "panPresent", normalizedPan != null));
    emit(
        "BUSINESS_SETTINGS_CHANGED",
        id,
        business.getOwnerUserId(),
        Map.of("change", "taxIdentifiers"));
    return business;
  }

  @Transactional
  public BusinessTaxProfile updateTaxProfile(
      String id,
      boolean gstRegistered,
      boolean compositionScheme,
      boolean reverseChargeEnabled,
      String defaultPlaceOfSupply,
      boolean tdsApplicable) {
    get(id);
    Instant now = clock.instant();
    BusinessTaxProfile profile =
        taxProfiles
            .findById(id)
            .orElseGet(() -> new BusinessTaxProfile(id, defaultPlaceOfSupply, now));
    profile.update(
        gstRegistered,
        compositionScheme,
        reverseChargeEnabled,
        defaultPlaceOfSupply,
        tdsApplicable,
        now);
    taxProfiles.save(profile);
    audit.record("TAX_PROFILE_UPDATED", "business", id, Map.of("gstRegistered", gstRegistered));
    emit("BUSINESS_SETTINGS_CHANGED", id, get(id).getOwnerUserId(), Map.of("change", "taxProfile"));
    return profile;
  }

  @Transactional(readOnly = true)
  public BusinessTaxProfile getTaxProfile(String id) {
    return taxProfiles.findById(id).orElseThrow(() -> ApiException.notFound("Tax profile"));
  }

  @Transactional
  public BusinessSettings updateSettings(
      String id, String invoicePrefix, String upiId, String logoUrl) {
    get(id);
    Instant now = clock.instant();
    BusinessSettings s = settings.findById(id).orElseGet(() -> new BusinessSettings(id, now));
    s.update(invoicePrefix, upiId, logoUrl, now);
    settings.save(s);
    audit.record("BUSINESS_SETTINGS_UPDATED", "business", id, Map.of());
    emit("BUSINESS_SETTINGS_CHANGED", id, get(id).getOwnerUserId(), Map.of("change", "settings"));
    return s;
  }

  @Transactional(readOnly = true)
  public BusinessSettings getSettings(String id) {
    return settings.findById(id).orElseThrow(() -> ApiException.notFound("Settings"));
  }

  @Transactional
  public Branch addBranch(String id, String name, String stateCode, String address) {
    get(id);
    Branch branch = new Branch(Ulid.newId(), id, name, stateCode, address, clock.instant());
    branches.save(branch);
    audit.record("BRANCH_CREATED", "branch", branch.getId(), Map.of("name", name));
    return branch;
  }

  @Transactional(readOnly = true)
  public List<Branch> listBranches(String id) {
    return branches.findByTenantId(id);
  }

  private static boolean isPresent(String s) {
    return s != null && !s.isBlank();
  }

  private void enrichContext(String tenantId, String actorId) {
    String correlationId =
        TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null);
    TenantContext.set(new TenantContext.Principal(tenantId, tenantId, actorId, correlationId));
  }

  private void emit(String eventType, String tenantId, String actorId, Map<String, ?> data) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService("identity-service")
            .actorId(actorId)
            .aggregateId(tenantId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(objectMapper.valueToTree(data))
            .build(clock.instant());
    outbox.append(envelope);
  }
}
