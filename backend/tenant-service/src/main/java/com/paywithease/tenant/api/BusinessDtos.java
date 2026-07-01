package com.paywithease.tenant.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** Request/response DTOs for the /businesses API. */
public final class BusinessDtos {

  private BusinessDtos() {}

  public record CreateBusiness(
      @NotBlank String legalName,
      String tradeName,
      @NotBlank String businessType,
      @NotBlank @Pattern(regexp = "\\d{2}", message = "stateCode must be a 2-digit GST state code")
          String stateCode) {}

  public record UpdateProfile(String legalName, String tradeName, String businessType) {}

  public record TaxIdentifiers(String gstin, String pan, String udyam) {}

  public record TaxProfileRequest(
      boolean gstRegistered,
      boolean compositionScheme,
      boolean reverseChargeEnabled,
      @Pattern(regexp = "\\d{2}", message = "invalid state code") String defaultPlaceOfSupply,
      boolean tdsApplicable) {}

  public record SettingsRequest(String invoicePrefix, String upiId, String logoUrl) {}

  public record BranchRequest(
      @NotBlank String name,
      @NotBlank @Pattern(regexp = "\\d{2}") String stateCode,
      String address) {}

  public record BusinessResponse(
      String id,
      String ownerUserId,
      String legalName,
      String tradeName,
      String businessType,
      String gstin,
      String pan,
      String udyam,
      String stateCode,
      String status) {}

  public record BranchResponse(
      String id, String tenantId, String name, String stateCode, String address) {}

  public record TaxProfileResponse(
      boolean gstRegistered,
      boolean compositionScheme,
      boolean reverseChargeEnabled,
      String defaultPlaceOfSupply,
      boolean tdsApplicable) {}

  public record SettingsResponse(
      String invoicePrefix,
      long invoiceNextNumber,
      String upiId,
      String logoUrl,
      String currency,
      int financialYearStartMonth) {}

  public record BranchList(List<BranchResponse> branches) {}
}
