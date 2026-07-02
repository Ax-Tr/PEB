package com.paywithease.payout.api;

import com.paywithease.payout.application.PayoutService;
import com.paywithease.payout.domain.Payout;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Vendor/employee payouts with maker-checker approval and step-up auth. */
@RestController
@RequestMapping("/api/v1/payouts")
@Tag(name = "payouts", description = "Payouts with approval, step-up, and gateway failover")
public class PayoutController {

  private final PayoutService service;

  public PayoutController(PayoutService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a payout (idempotent; high-risk requires X-Step-Up-Verified)")
  public PayoutDtos.CreatePayoutResponse create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestHeader(value = "X-Step-Up-Verified", required = false, defaultValue = "false")
          boolean stepUpVerified,
      @Valid @RequestBody PayoutDtos.CreatePayout body,
      @AuthenticationPrincipal Jwt jwt) {
    PayoutService.CreateResult r =
        service.createPayout(
            idempotencyKey,
            new PayoutService.CreateCommand(
                body.partyType(),
                body.partyId(),
                body.beneficiaryId(),
                body.amountMinor(),
                body.purpose()),
            jwt.getSubject(),
            stepUpVerified);
    return new PayoutDtos.CreatePayoutResponse(
        r.payoutId(), r.status(), r.riskLevel(), r.requiresApproval());
  }

  @GetMapping("/{id}")
  public PayoutDtos.PayoutResponse get(@PathVariable String id) {
    return toResponse(service.get(id));
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Approve a pending payout (maker cannot approve their own)")
  public PayoutDtos.PayoutResponse approve(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return toResponse(service.approve(id, jwt.getSubject()));
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Reject a pending payout")
  public PayoutDtos.PayoutResponse reject(
      @PathVariable String id,
      @RequestBody(required = false) PayoutDtos.RejectRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    String reason = body == null ? null : body.reason();
    return toResponse(service.reject(id, jwt.getSubject(), reason));
  }

  private static PayoutDtos.PayoutResponse toResponse(Payout p) {
    return new PayoutDtos.PayoutResponse(
        p.getId(),
        p.getPartyType(),
        p.getPartyId(),
        p.getBeneficiaryId(),
        p.getAmountMinor(),
        p.getStatus().name(),
        p.getRiskLevel().name(),
        p.getProvider(),
        p.getProviderRef());
  }
}
