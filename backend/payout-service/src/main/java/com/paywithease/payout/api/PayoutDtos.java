package com.paywithease.payout.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/** Request/response DTOs for the payout API. */
public final class PayoutDtos {

  private PayoutDtos() {}

  public record RegisterBeneficiary(
      @NotBlank String partyType,
      @NotBlank String partyId,
      String label,
      @NotBlank String accountNumber,
      boolean verified) {}

  public record BeneficiaryResponse(
      String id, String partyType, String partyId, String label, Instant verifiedAt) {}

  public record CreatePayout(
      @NotBlank String partyType,
      @NotBlank String partyId,
      @NotBlank String beneficiaryId,
      @Positive long amountMinor,
      String purpose) {}

  public record PayoutResponse(
      String id,
      String partyType,
      String partyId,
      String beneficiaryId,
      long amountMinor,
      String status,
      String riskLevel,
      String provider,
      String providerRef) {}

  public record CreatePayoutResponse(
      String payoutId, String status, String riskLevel, boolean requiresApproval) {}

  public record RejectRequest(String reason) {}
}
