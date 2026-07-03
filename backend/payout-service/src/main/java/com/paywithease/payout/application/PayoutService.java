package com.paywithease.payout.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.idempotency.IdempotencyService;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.payout.domain.Beneficiary;
import com.paywithease.payout.domain.PartyType;
import com.paywithease.payout.domain.Payout;
import com.paywithease.payout.domain.PayoutApproval;
import com.paywithease.payout.domain.RiskLevel;
import com.paywithease.payout.infrastructure.BeneficiaryRepository;
import com.paywithease.payout.infrastructure.PayoutApprovalRepository;
import com.paywithease.payout.infrastructure.PayoutRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vendor/employee payouts with fintech-grade controls: beneficiary validation, risk-based
 * maker-checker approval, step-up auth for high-risk actions, idempotent creation, and gateway
 * failover. The maker can never approve their own payout.
 */
@Service
public class PayoutService {

  private static final String SOURCE = "payout-service";

  private final BeneficiaryRepository beneficiaries;
  private final PayoutRepository payouts;
  private final PayoutApprovalRepository approvals;
  private final IdempotencyService idempotency;
  private final RiskAssessor riskAssessor;
  private final PayoutGatewayRouter gatewayRouter;
  private final BlindIndex blindIndex;
  private final AuditWriter audit;
  private final com.paywithease.common.outbox.OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public PayoutService(
      BeneficiaryRepository beneficiaries,
      PayoutRepository payouts,
      PayoutApprovalRepository approvals,
      IdempotencyService idempotency,
      RiskAssessor riskAssessor,
      PayoutGatewayRouter gatewayRouter,
      BlindIndex blindIndex,
      AuditWriter audit,
      com.paywithease.common.outbox.OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.beneficiaries = beneficiaries;
    this.payouts = payouts;
    this.approvals = approvals;
    this.idempotency = idempotency;
    this.riskAssessor = riskAssessor;
    this.gatewayRouter = gatewayRouter;
    this.blindIndex = blindIndex;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  // ---- Beneficiaries ---------------------------------------------------------

  @Transactional
  public Beneficiary registerBeneficiary(
      String partyTypeRaw, String partyId, String label, String accountNumber, boolean verified) {
    String tenantId = TenantContext.requireTenantId();
    if (!PartyType.isValid(partyTypeRaw)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown partyType: " + partyTypeRaw);
    }
    Instant now = clock.instant();
    Beneficiary beneficiary =
        new Beneficiary(
            Ulid.newId(),
            tenantId,
            PartyType.valueOf(partyTypeRaw),
            partyId,
            label,
            blindIndex.hash(accountNumber),
            verified ? now : null,
            now);
    beneficiaries.save(beneficiary);
    audit.record(
        "BENEFICIARY_REGISTERED",
        "beneficiary",
        beneficiary.getId(),
        Map.of("partyType", partyTypeRaw, "verified", verified));
    return beneficiary;
  }

  // ---- Payout creation -------------------------------------------------------

  public record CreateCommand(
      String partyType, String partyId, String beneficiaryId, long amountMinor, String purpose) {}

  public record CreateResult(
      String payoutId, String status, String riskLevel, boolean requiresApproval) {}

  /**
   * Creates a payout. High-risk payouts require prior step-up auth and go to PENDING_APPROVAL;
   * low-risk payouts are auto-approved and initiated. Idempotent on the supplied key.
   */
  @Transactional
  public CreateResult createPayout(
      String idempotencyKey, CreateCommand cmd, String createdBy, boolean stepUpVerified) {
    String tenantId = TenantContext.requireTenantId();
    String requestHash = idempotency.hashRequest(cmd);
    return idempotency.execute(
        tenantId,
        idempotencyKey,
        "POST /payouts",
        requestHash,
        CreateResult.class,
        () -> doCreate(tenantId, cmd, createdBy, stepUpVerified));
  }

  private CreateResult doCreate(
      String tenantId, CreateCommand cmd, String createdBy, boolean stepUpVerified) {
    if (!PartyType.isValid(cmd.partyType())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown partyType: " + cmd.partyType());
    }
    Beneficiary beneficiary =
        beneficiaries
            .findByTenantIdAndId(tenantId, cmd.beneficiaryId())
            .orElseThrow(() -> ApiException.notFound("Beneficiary"));
    if (!beneficiary.isActive()) {
      throw new ApiException(ErrorCode.CONFLICT, "Beneficiary is not active");
    }

    RiskLevel risk = riskAssessor.assess(cmd.amountMinor(), beneficiary);

    // Step-up authentication is mandatory before a high-risk payout can even be created.
    if (risk == RiskLevel.HIGH && !stepUpVerified) {
      throw new ApiException(
          ErrorCode.STEP_UP_REQUIRED, "Step-up authentication required for this payout");
    }

    boolean requiresApproval = risk == RiskLevel.HIGH;
    Instant now = clock.instant();
    Payout payout =
        new Payout(
            Ulid.newId(),
            tenantId,
            PartyType.valueOf(cmd.partyType()),
            cmd.partyId(),
            beneficiary.getId(),
            cmd.amountMinor(),
            cmd.purpose(),
            risk,
            requiresApproval,
            createdBy,
            now);
    payouts.save(payout);
    audit.record(
        "PAYOUT_CREATED",
        "payout",
        payout.getId(),
        Map.of(
            "amountMinor",
            cmd.amountMinor(),
            "risk",
            risk.name(),
            "requiresApproval",
            requiresApproval));

    if (requiresApproval) {
      emit("PAYOUT_APPROVAL_REQUESTED", payout, Map.of("risk", risk.name()));
    } else {
      initiate(payout);
    }
    return new CreateResult(
        payout.getId(), payout.getStatus().name(), risk.name(), requiresApproval);
  }

  // ---- Approval (maker-checker) ---------------------------------------------

  @Transactional
  public Payout approve(String payoutId, String approverId) {
    Payout payout = load(payoutId);
    enforceDifferentApprover(payout, approverId);
    payout.approve(clock.instant());
    payouts.save(payout);
    approvals.save(
        new PayoutApproval(
            Ulid.newId(),
            payout.getTenantId(),
            payoutId,
            "APPROVED",
            approverId,
            null,
            clock.instant()));
    audit.record("PAYOUT_APPROVED", "payout", payoutId, Map.of("approver", approverId));
    emit(
        "PAYOUT_APPROVAL_COMPLETED",
        payout,
        Map.of("decision", "APPROVED", "approver", approverId));
    initiate(payout);
    return payout;
  }

  @Transactional
  public Payout reject(String payoutId, String approverId, String reason) {
    Payout payout = load(payoutId);
    enforceDifferentApprover(payout, approverId);
    payout.reject(clock.instant());
    payouts.save(payout);
    approvals.save(
        new PayoutApproval(
            Ulid.newId(),
            payout.getTenantId(),
            payoutId,
            "REJECTED",
            approverId,
            reason,
            clock.instant()));
    audit.record("PAYOUT_REJECTED", "payout", payoutId, Map.of("approver", approverId));
    emit(
        "PAYOUT_APPROVAL_COMPLETED",
        payout,
        Map.of("decision", "REJECTED", "approver", approverId));
    return payout;
  }

  private void enforceDifferentApprover(Payout payout, String approverId) {
    if (approverId != null && approverId.equals(payout.getCreatedBy())) {
      throw new ApiException(ErrorCode.FORBIDDEN, "Maker cannot approve their own payout");
    }
  }

  // ---- Disbursement ----------------------------------------------------------

  private void initiate(Payout payout) {
    Instant now = clock.instant();
    PayoutGatewayRouter.RoutedResult routed =
        gatewayRouter.disburse(
            payout.getBeneficiaryId(), payout.getAmountMinor(), "payout:" + payout.getId());
    for (int i = 0; i < routed.attempts(); i++) {
      payout.incrementAttempts();
    }
    if (!routed.success()) {
      payout.markFailed(now);
      payouts.save(payout);
      audit.record(
          "PAYOUT_FAILED", "payout", payout.getId(), Map.of("attempts", routed.attempts()));
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "All payout gateways failed");
    }
    payout.markInitiated(routed.provider(), routed.providerRef(), now);
    payouts.save(payout);
    emit("VENDOR_PAYMENT_INITIATED", payout, Map.of("provider", routed.provider()));

    // Sprint 6 models synchronous settlement; async webhook confirmation is a later refinement.
    payout.markCompleted(now);
    payouts.save(payout);
    emit(
        "VENDOR_PAYMENT_COMPLETED",
        payout,
        Map.of(
            "amountMinor",
            payout.getAmountMinor(),
            "partyId",
            payout.getPartyId(),
            "payoutId",
            payout.getId()));
    audit.record(
        "VENDOR_PAYMENT_COMPLETED",
        "payout",
        payout.getId(),
        Map.of("provider", routed.provider()));
  }

  @Transactional(readOnly = true)
  public Payout get(String id) {
    return load(id);
  }

  @Transactional(readOnly = true)
  public List<Payout> list() {
    return payouts.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  private Payout load(String id) {
    return payouts
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Payout"));
  }

  private void emit(String eventType, Payout payout, Map<String, ?> extra) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("payoutId", payout.getId());
    payload.put("partyType", payout.getPartyType());
    payload.put("partyId", payout.getPartyId());
    payload.put("amountMinor", payout.getAmountMinor());
    payload.put("status", payout.getStatus().name());
    payload.setAll((ObjectNode) objectMapper.valueToTree(extra));
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(payout.getTenantId())
            .businessId(payout.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(payout.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
