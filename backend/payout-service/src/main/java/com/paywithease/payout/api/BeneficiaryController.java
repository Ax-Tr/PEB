package com.paywithease.payout.api;

import com.paywithease.common.tenant.TenantContext;
import com.paywithease.payout.application.PayoutService;
import com.paywithease.payout.domain.Beneficiary;
import com.paywithease.payout.domain.PartyType;
import com.paywithease.payout.infrastructure.BeneficiaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Register and list verified payout destinations. */
@RestController
@RequestMapping("/api/v1/beneficiaries")
@Tag(name = "beneficiaries", description = "Verified payout destinations")
public class BeneficiaryController {

  private final PayoutService service;
  private final BeneficiaryRepository beneficiaries;

  public BeneficiaryController(PayoutService service, BeneficiaryRepository beneficiaries) {
    this.service = service;
    this.beneficiaries = beneficiaries;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a beneficiary (bank details are stored as a blind index)")
  public PayoutDtos.BeneficiaryResponse register(
      @Valid @RequestBody PayoutDtos.RegisterBeneficiary body) {
    Beneficiary b =
        service.registerBeneficiary(
            body.partyType(), body.partyId(), body.label(), body.accountNumber(), body.verified());
    return toResponse(b);
  }

  @GetMapping
  public List<PayoutDtos.BeneficiaryResponse> list(
      @RequestParam String partyType, @RequestParam String partyId) {
    if (!PartyType.isValid(partyType)) {
      return List.of();
    }
    return beneficiaries
        .findByTenantIdAndPartyTypeAndPartyId(TenantContext.requireTenantId(), partyType, partyId)
        .stream()
        .map(BeneficiaryController::toResponse)
        .toList();
  }

  private static PayoutDtos.BeneficiaryResponse toResponse(Beneficiary b) {
    return new PayoutDtos.BeneficiaryResponse(
        b.getId(), b.getPartyType(), b.getPartyId(), b.getLabel(), b.getVerifiedAt());
  }
}
