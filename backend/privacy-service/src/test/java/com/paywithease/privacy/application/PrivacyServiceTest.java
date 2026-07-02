package com.paywithease.privacy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.privacy.domain.DataCategory;
import com.paywithease.privacy.domain.DsrRequest;
import com.paywithease.privacy.domain.DsrType;
import com.paywithease.privacy.infrastructure.DsrRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
class PrivacyServiceTest {

  @Mock DsrRequestRepository requests;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private PrivacyService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service = new PrivacyService(requests, audit, outbox, objectMapper, clock, 30);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "dpo1", "corr1"));
    when(requests.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void submitCreatesReceivedRequestWithSlaAndEmits() {
    DsrRequest r = service.submitRequest(DsrType.ERASURE, "subj1", "user@example.com", "erase me");
    assertThat(r.getStatus()).isEqualTo("RECEIVED");
    assertThat(r.getDueAt()).isEqualTo(Instant.parse("2026-07-31T00:00:00Z")); // +30 days
    verify(outbox).append(any());
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void grievanceEmitsGrievanceEvent() {
    service.submitRequest(DsrType.GRIEVANCE, null, "user@example.com", "complaint");
    // one DSR_RECEIVED + one DPDP_GRIEVANCE_RAISED
    verify(outbox, org.mockito.Mockito.times(2)).append(any());
  }

  @Test
  void planErasureRetainsFinancialAndEmitsErasureRequested() {
    DsrRequest r = inProgress();
    when(requests.findByTenantIdAndId("tenant1", "r1")).thenReturn(Optional.of(r));

    var plan =
        service.planErasure(
            "r1",
            List.of(DataCategory.MARKETING, DataCategory.FINANCIAL_TXN, DataCategory.KYC_PII));

    assertThat(plan.fullErasurePossible()).isFalse();
    assertThat(plan.summary()).contains("legal hold");
    verify(outbox).append(any()); // DATA_ERASURE_REQUESTED
    assertThat(r.getErasurePlan()).isNotBlank();
  }

  @Test
  void fullLifecycleThroughService() {
    DsrRequest r =
        new DsrRequest(
            "r1",
            "tenant1",
            DsrType.ERASURE,
            "subj1",
            "user@example.com",
            "erase",
            clock.instant(),
            clock.instant().plusSeconds(100));
    when(requests.findByTenantIdAndId("tenant1", "r1")).thenReturn(Optional.of(r));

    service.startVerification("r1");
    service.markVerified("r1");
    service.planErasure("r1", List.of(DataCategory.MARKETING));
    DsrRequest done = service.completeRequest("r1", "s3://evidence.zip", "done");
    assertThat(done.getStatus()).isEqualTo("COMPLETED");
  }

  @Test
  void crossTenantAccessIsBlocked() {
    when(requests.findByTenantIdAndId("tenant2", "r1")).thenReturn(Optional.empty());
    TenantContext.set(new TenantContext.Principal("tenant2", "tenant2", "dpo2", "corr2"));
    assertThatThrownBy(() -> service.get("r1")).isInstanceOf(ApiException.class);
  }

  private DsrRequest inProgress() {
    DsrRequest r =
        new DsrRequest(
            "r1",
            "tenant1",
            DsrType.ERASURE,
            "subj1",
            "user@example.com",
            "erase",
            clock.instant(),
            clock.instant().plusSeconds(100));
    r.startVerification();
    r.markVerified("dpo1", clock.instant());
    return r;
  }
}
