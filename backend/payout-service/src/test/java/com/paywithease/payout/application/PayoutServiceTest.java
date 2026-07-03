package com.paywithease.payout.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.idempotency.IdempotencyService;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.payout.domain.Beneficiary;
import com.paywithease.payout.domain.PartyType;
import com.paywithease.payout.domain.Payout;
import com.paywithease.payout.domain.PayoutStatus;
import com.paywithease.payout.domain.RiskLevel;
import com.paywithease.payout.infrastructure.BeneficiaryRepository;
import com.paywithease.payout.infrastructure.PayoutApprovalRepository;
import com.paywithease.payout.infrastructure.PayoutRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayoutServiceTest {

  @Mock BeneficiaryRepository beneficiaries;
  @Mock PayoutRepository payouts;
  @Mock PayoutApprovalRepository approvals;
  @Mock IdempotencyService idempotency;
  @Mock RiskAssessor riskAssessor;
  @Mock PayoutGatewayRouter gatewayRouter;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private PayoutService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new PayoutService(
            beneficiaries,
            payouts,
            approvals,
            idempotency,
            riskAssessor,
            gatewayRouter,
            new BlindIndex(new byte[32]),
            audit,
            outbox,
            objectMapper,
            clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(idempotency.hashRequest(any())).thenReturn("h");
    when(idempotency.execute(
            anyString(),
            anyString(),
            anyString(),
            anyString(),
            eq(PayoutService.CreateResult.class),
            any()))
        .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(5)).get());
    when(payouts.save(any())).thenAnswer(returnsFirstArg());
    when(approvals.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private Beneficiary activeBeneficiary() {
    return new Beneficiary(
        "ben1",
        "tenant1",
        PartyType.VENDOR,
        "vendor1",
        "acme",
        "hash",
        clock.instant().minusSeconds(30L * 24 * 3600),
        clock.instant());
  }

  private PayoutService.CreateCommand cmd(long amount) {
    return new PayoutService.CreateCommand("VENDOR", "vendor1", "ben1", amount, "supplies");
  }

  @Test
  void lowRiskPayoutAutoInitiatesAndCompletes() {
    when(beneficiaries.findByTenantIdAndId("tenant1", "ben1"))
        .thenReturn(Optional.of(activeBeneficiary()));
    when(riskAssessor.assess(anyLong(), any())).thenReturn(RiskLevel.LOW);
    when(gatewayRouter.disburse(any(), anyLong(), any()))
        .thenReturn(new PayoutGatewayRouter.RoutedResult(true, "razorpayx", "ref1", 1));

    PayoutService.CreateResult r = service.createPayout("idem1", cmd(100000), "maker", false);

    assertThat(r.requiresApproval()).isFalse();
    assertThat(r.status()).isEqualTo(PayoutStatus.COMPLETED.name());
    verify(gatewayRouter).disburse(any(), anyLong(), any());
    verify(outbox, times(2)).append(any()); // VENDOR_PAYMENT_INITIATED + COMPLETED
  }

  @Test
  void highRiskWithoutStepUpIsRejected() {
    when(beneficiaries.findByTenantIdAndId("tenant1", "ben1"))
        .thenReturn(Optional.of(activeBeneficiary()));
    when(riskAssessor.assess(anyLong(), any())).thenReturn(RiskLevel.HIGH);

    assertThatThrownBy(() -> service.createPayout("idem1", cmd(9_000_000), "maker", false))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Step-up");
    verify(payouts, never()).save(any());
    verify(gatewayRouter, never()).disburse(any(), anyLong(), any());
  }

  @Test
  void highRiskWithStepUpGoesToApproval() {
    when(beneficiaries.findByTenantIdAndId("tenant1", "ben1"))
        .thenReturn(Optional.of(activeBeneficiary()));
    when(riskAssessor.assess(anyLong(), any())).thenReturn(RiskLevel.HIGH);

    PayoutService.CreateResult r = service.createPayout("idem1", cmd(9_000_000), "maker", true);

    assertThat(r.requiresApproval()).isTrue();
    assertThat(r.status()).isEqualTo(PayoutStatus.PENDING_APPROVAL.name());
    verify(gatewayRouter, never())
        .disburse(any(), anyLong(), any()); // not disbursed until approved
    verify(outbox).append(any()); // only PAYOUT_APPROVAL_REQUESTED
  }

  @Test
  void approvalByDifferentUserInitiatesDisbursement() {
    Payout pending =
        new Payout(
            "po1",
            "tenant1",
            PartyType.VENDOR,
            "vendor1",
            "ben1",
            9_000_000,
            "supplies",
            RiskLevel.HIGH,
            true,
            "maker",
            clock.instant());
    when(payouts.findByTenantIdAndId("tenant1", "po1")).thenReturn(Optional.of(pending));
    when(gatewayRouter.disburse(any(), anyLong(), any()))
        .thenReturn(new PayoutGatewayRouter.RoutedResult(true, "razorpayx", "ref1", 1));

    Payout result = service.approve("po1", "checker");

    assertThat(result.getStatus()).isEqualTo(PayoutStatus.COMPLETED);
    verify(approvals).save(any());
    verify(outbox, times(3)).append(any()); // APPROVAL_COMPLETED + INITIATED + COMPLETED
  }

  @Test
  void makerCannotApproveOwnPayout() {
    Payout pending =
        new Payout(
            "po1",
            "tenant1",
            PartyType.VENDOR,
            "vendor1",
            "ben1",
            9_000_000,
            "supplies",
            RiskLevel.HIGH,
            true,
            "maker",
            clock.instant());
    when(payouts.findByTenantIdAndId("tenant1", "po1")).thenReturn(Optional.of(pending));

    assertThatThrownBy(() -> service.approve("po1", "maker"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Maker cannot approve");
    verify(gatewayRouter, never()).disburse(any(), anyLong(), any());
  }

  @Test
  void listReturnsTenantPayouts() {
    Payout payout =
        new Payout(
            "po1",
            "tenant1",
            PartyType.VENDOR,
            "vendor1",
            "ben1",
            100000,
            "supplies",
            RiskLevel.LOW,
            false,
            "maker",
            clock.instant());
    when(payouts.findByTenantIdOrderByCreatedAtDesc("tenant1")).thenReturn(List.of(payout));

    List<Payout> result = service.list();

    assertThat(result).containsExactly(payout);
  }

  @Test
  void unknownBeneficiaryIsRejected() {
    when(beneficiaries.findByTenantIdAndId("tenant1", "ben1")).thenReturn(Optional.empty());
    when(riskAssessor.assess(anyLong(), any())).thenReturn(RiskLevel.LOW);
    assertThatThrownBy(() -> service.createPayout("idem1", cmd(1000), "maker", false))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Beneficiary");
  }
}
