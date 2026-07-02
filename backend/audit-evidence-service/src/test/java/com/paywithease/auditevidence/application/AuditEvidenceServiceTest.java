package com.paywithease.auditevidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.auditevidence.domain.EvidenceIntegrity;
import com.paywithease.auditevidence.domain.EvidenceItem;
import com.paywithease.auditevidence.domain.ExportJob;
import com.paywithease.auditevidence.infrastructure.EvidenceItemRepository;
import com.paywithease.auditevidence.infrastructure.ExportJobRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
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
class AuditEvidenceServiceTest {

  @Mock EvidenceItemRepository evidence;
  @Mock ExportJobRepository exports;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private AuditEvidenceService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service = new AuditEvidenceService(evidence, exports, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(evidence.save(any())).thenAnswer(returnsFirstArg());
    when(exports.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void recordUploadedEvidenceAppendsAndEmitsAudit() {
    when(evidence.existsByTenantIdAndEntityTypeAndEntityIdAndContentHash(
            any(), any(), any(), any()))
        .thenReturn(false);

    EvidenceItem item =
        service.recordUploadedEvidence(
            "INVOICE", "inv1", "bytes".getBytes(StandardCharsets.UTF_8), "s3://x", "scan");

    assertThat(item.getContentHash())
        .isEqualTo(EvidenceIntegrity.sha256Hex("bytes".getBytes(StandardCharsets.UTF_8)));
    assertThat(item.getSource()).isEqualTo("UPLOAD");
    verify(evidence).save(any(EvidenceItem.class));
    verify(outbox).append(any());
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void recordEvidenceIsIdempotentOnSameHash() {
    byte[] content = "bytes".getBytes(StandardCharsets.UTF_8);
    String hash = EvidenceIntegrity.sha256Hex(content);
    EvidenceItem existing =
        new EvidenceItem(
            "e1",
            "tenant1",
            "INVOICE",
            "inv1",
            hash,
            null,
            "scan",
            "UPLOAD",
            "actor1",
            clock.instant());
    when(evidence.existsByTenantIdAndEntityTypeAndEntityIdAndContentHash(
            "tenant1", "INVOICE", "inv1", hash))
        .thenReturn(true);
    when(evidence.findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            "tenant1", "INVOICE", "inv1"))
        .thenReturn(List.of(existing));

    EvidenceItem item = service.recordUploadedEvidence("INVOICE", "inv1", content, null, "scan");

    assertThat(item.getId()).isEqualTo("e1");
    verify(evidence, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void verifyIntegrityDetectsTamper() {
    byte[] content = "orig".getBytes(StandardCharsets.UTF_8);
    EvidenceItem item =
        new EvidenceItem(
            "e1",
            "tenant1",
            "INVOICE",
            "inv1",
            EvidenceIntegrity.sha256Hex(content),
            null,
            null,
            "UPLOAD",
            "actor1",
            clock.instant());
    when(evidence.findByTenantIdAndId("tenant1", "e1")).thenReturn(Optional.of(item));

    assertThat(service.verifyIntegrity("e1", content).valid()).isTrue();
    assertThat(service.verifyIntegrity("e1", "tampered".getBytes(StandardCharsets.UTF_8)).valid())
        .isFalse();
  }

  @Test
  void exportLifecycle() {
    ExportJob job = new ExportJob("j1", "tenant1", "scope", "actor1", clock.instant());
    when(exports.findByTenantIdAndId("tenant1", "j1")).thenReturn(Optional.of(job));

    service.startExport("j1");
    ExportJob done = service.completeExport("j1", "s3://out.zip");
    assertThat(done.getStatus()).isEqualTo("COMPLETED");
  }

  @Test
  void serviceExposesNoDeleteOrUpdateOfEvidence() {
    // Immutability guardrail: the service surface must never expose a way to mutate evidence.
    boolean hasMutator =
        java.util.Arrays.stream(AuditEvidenceService.class.getDeclaredMethods())
            .anyMatch(
                m -> {
                  String n = m.getName().toLowerCase();
                  return (n.contains("delete") || n.contains("update") || n.contains("edit"))
                      && n.contains("evidence");
                });
    assertThat(hasMutator).isFalse();
  }
}
