package com.paywithease.identity.api;

import com.paywithease.common.error.ApiException;
import com.paywithease.identity.application.BusinessService;
import com.paywithease.identity.domain.tenant.Branch;
import com.paywithease.identity.domain.tenant.Business;
import com.paywithease.identity.domain.tenant.BusinessSettings;
import com.paywithease.identity.domain.tenant.BusinessTaxProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Business onboarding & settings API. */
@RestController
@RequestMapping("/api/v1/businesses")
@Tag(name = "businesses", description = "Business profile, tax identifiers, branches, settings")
public class BusinessController {

  private final BusinessService service;

  public BusinessController(BusinessService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('OWNER')")
  @Operation(summary = "Create a business (the caller becomes owner)")
  public BusinessDtos.BusinessResponse create(
      @Valid @RequestBody BusinessDtos.CreateBusiness body, @AuthenticationPrincipal Jwt jwt) {
    Business b =
        service.create(
            jwt.getSubject(),
            body.legalName(),
            body.tradeName(),
            body.businessType(),
            body.stateCode());
    return toResponse(b);
  }

  @GetMapping("/{id}")
  public BusinessDtos.BusinessResponse get(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return toResponse(requireOwner(id, jwt));
  }

  @PatchMapping("/{id}")
  public BusinessDtos.BusinessResponse updateProfile(
      @PathVariable String id,
      @Valid @RequestBody BusinessDtos.UpdateProfile body,
      @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return toResponse(
        service.updateProfile(id, body.legalName(), body.tradeName(), body.businessType()));
  }

  @PutMapping("/{id}/tax-identifiers")
  public BusinessDtos.BusinessResponse setTaxIdentifiers(
      @PathVariable String id,
      @Valid @RequestBody BusinessDtos.TaxIdentifiers body,
      @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return toResponse(service.setTaxIdentifiers(id, body.gstin(), body.pan(), body.udyam()));
  }

  @PutMapping("/{id}/tax-profile")
  public BusinessDtos.TaxProfileResponse updateTaxProfile(
      @PathVariable String id,
      @Valid @RequestBody BusinessDtos.TaxProfileRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return toTaxProfile(
        service.updateTaxProfile(
            id,
            body.gstRegistered(),
            body.compositionScheme(),
            body.reverseChargeEnabled(),
            body.defaultPlaceOfSupply(),
            body.tdsApplicable()));
  }

  @GetMapping("/{id}/tax-profile")
  public BusinessDtos.TaxProfileResponse getTaxProfile(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return toTaxProfile(service.getTaxProfile(id));
  }

  @PutMapping("/{id}/settings")
  public BusinessDtos.SettingsResponse updateSettings(
      @PathVariable String id,
      @Valid @RequestBody BusinessDtos.SettingsRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return toSettings(
        service.updateSettings(id, body.invoicePrefix(), body.upiId(), body.logoUrl()));
  }

  @GetMapping("/{id}/settings")
  public BusinessDtos.SettingsResponse getSettings(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return toSettings(service.getSettings(id));
  }

  @PostMapping("/{id}/branches")
  @ResponseStatus(HttpStatus.CREATED)
  public BusinessDtos.BranchResponse addBranch(
      @PathVariable String id,
      @Valid @RequestBody BusinessDtos.BranchRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return toBranch(service.addBranch(id, body.name(), body.stateCode(), body.address()));
  }

  @GetMapping("/{id}/branches")
  public BusinessDtos.BranchList listBranches(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    requireOwner(id, jwt);
    return new BusinessDtos.BranchList(
        service.listBranches(id).stream().map(this::toBranch).toList());
  }

  private Business requireOwner(String id, Jwt jwt) {
    Business b = service.get(id);
    if (!b.getOwnerUserId().equals(jwt.getSubject())) {
      throw ApiException.forbidden();
    }
    return b;
  }

  private BusinessDtos.BusinessResponse toResponse(Business b) {
    return new BusinessDtos.BusinessResponse(
        b.getId(),
        b.getOwnerUserId(),
        b.getLegalName(),
        b.getTradeName(),
        b.getBusinessType(),
        b.getGstin(),
        b.getPan(),
        b.getUdyam(),
        b.getStateCode(),
        b.getStatus());
  }

  private BusinessDtos.TaxProfileResponse toTaxProfile(BusinessTaxProfile p) {
    return new BusinessDtos.TaxProfileResponse(
        p.isGstRegistered(),
        p.isCompositionScheme(),
        p.isReverseChargeEnabled(),
        p.getDefaultPlaceOfSupply(),
        p.isTdsApplicable());
  }

  private BusinessDtos.SettingsResponse toSettings(BusinessSettings s) {
    return new BusinessDtos.SettingsResponse(
        s.getInvoicePrefix(),
        s.getInvoiceNextNumber(),
        s.getUpiId(),
        s.getLogoUrl(),
        s.getCurrency(),
        s.getFinancialYearStartMonth());
  }

  private BusinessDtos.BranchResponse toBranch(Branch b) {
    return new BusinessDtos.BranchResponse(
        b.getId(), b.getTenantId(), b.getName(), b.getStateCode(), b.getAddress());
  }
}
