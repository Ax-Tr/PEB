package com.paywithease.auditevidence.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.auditevidence.domain.EvidenceIntegrity;
import com.paywithease.auditevidence.domain.EvidenceItem;
import com.paywithease.auditevidence.domain.ExportJob;
import com.paywithease.auditevidence.infrastructure.EvidenceItemRepository;
import com.paywithease.auditevidence.infrastructure.ExportJobRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Immutable evidence room + auditor exports. Evidence is append-only — there is deliberately no
 * update or delete path — and every recorded item emits {@code AUDIT_EVENT_RECORDED}. Integrity can
 * be re-verified at any time against the stored SHA-256 hash.
 */
@Service
public class AuditEvidenceService {

  private static final String SOURCE = "audit-evidence-service";
  public static final String SOURCE_UPLOAD = "UPLOAD";
  public static final String SOURCE_SYSTEM = "SYSTEM_EVENT";

  private final EvidenceItemRepository evidence;
  private final ExportJobRepository exports;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public AuditEvidenceService(
      EvidenceItemRepository evidence,
      ExportJobRepository exports,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.evidence = evidence;
    this.exports = exports;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public record VerifyResult(String evidenceId, boolean valid, String storedHash) {}

  /** Record uploaded evidence: the SHA-256 hash is computed from the supplied content bytes. */
  @Transactional
  public EvidenceItem recordUploadedEvidence(
      String entityType, String entityId, byte[] content, String storageRef, String description) {
    String hash = EvidenceIntegrity.sha256Hex(content);
    return append(entityType, entityId, hash, storageRef, description, SOURCE_UPLOAD, actor());
  }

  /**
   * Record system-generated evidence (e.g. from a domain event) where only a canonical content hash
   * is available. Idempotent per (entity, hash) so replayed events do not duplicate evidence.
   */
  @Transactional
  public EvidenceItem recordSystemEvidence(
      String entityType, String entityId, String contentHash, String description) {
    return append(entityType, entityId, contentHash, null, description, SOURCE_SYSTEM, null);
  }

  private EvidenceItem append(
      String entityType,
      String entityId,
      String contentHash,
      String storageRef,
      String description,
      String source,
      String uploadedBy) {
    String tenantId = TenantContext.requireTenantId();
    if (evidence.existsByTenantIdAndEntityTypeAndEntityIdAndContentHash(
        tenantId, entityType, entityId, contentHash)) {
      // Identical evidence already recorded for this entity — return the existing (idempotent).
      return evidence
          .findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
              tenantId, entityType, entityId)
          .stream()
          .filter(e -> e.getContentHash().equals(contentHash))
          .findFirst()
          .orElseThrow(() -> ApiException.notFound("Evidence"));
    }
    EvidenceItem item =
        new EvidenceItem(
            Ulid.newId(),
            tenantId,
            entityType,
            entityId,
            contentHash,
            storageRef,
            description,
            source,
            uploadedBy,
            clock.instant());
    evidence.save(item);
    audit.record(
        "AUDIT_EVENT_RECORDED",
        "evidence_item",
        item.getId(),
        Map.of("entityType", entityType, "entityId", entityId, "source", source));
    emit(item);
    return item;
  }

  @Transactional(readOnly = true)
  public VerifyResult verifyIntegrity(String evidenceId, byte[] content) {
    EvidenceItem item = loadEvidence(evidenceId);
    boolean valid = EvidenceIntegrity.verify(item.getContentHash(), content);
    return new VerifyResult(evidenceId, valid, item.getContentHash());
  }

  @Transactional(readOnly = true)
  public List<EvidenceItem> listEvidence(String entityType, String entityId) {
    return evidence.findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        TenantContext.requireTenantId(), entityType, entityId);
  }

  @Transactional(readOnly = true)
  public EvidenceItem getEvidence(String evidenceId) {
    return loadEvidence(evidenceId);
  }

  // -------- Export jobs --------

  @Transactional
  public ExportJob requestExport(String scope) {
    ExportJob job =
        new ExportJob(
            Ulid.newId(), TenantContext.requireTenantId(), scope, actor(), clock.instant());
    exports.save(job);
    audit.record("EVIDENCE_EXPORT_REQUESTED", "export_job", job.getId(), Map.of("scope", scope));
    return job;
  }

  @Transactional
  public ExportJob startExport(String jobId) {
    ExportJob job = loadExport(jobId);
    job.start(clock.instant());
    exports.save(job);
    return job;
  }

  @Transactional
  public ExportJob completeExport(String jobId, String resultRef) {
    ExportJob job = loadExport(jobId);
    job.complete(resultRef, clock.instant());
    exports.save(job);
    audit.record("EVIDENCE_EXPORT_COMPLETED", "export_job", jobId, Map.of());
    return job;
  }

  @Transactional
  public ExportJob failExport(String jobId, String error) {
    ExportJob job = loadExport(jobId);
    job.fail(error, clock.instant());
    exports.save(job);
    return job;
  }

  @Transactional(readOnly = true)
  public List<ExportJob> listExports() {
    return exports.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  private EvidenceItem loadEvidence(String id) {
    return evidence
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Evidence"));
  }

  private ExportJob loadExport(String id) {
    return exports
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Export job"));
  }

  private String actor() {
    return TenantContext.actorId()
        .orElseThrow(
            () -> new ApiException(ErrorCode.UNAUTHENTICATED, "No acting user in context"));
  }

  private void emit(EvidenceItem item) {
    var payload =
        objectMapper
            .createObjectNode()
            .put("evidenceId", item.getId())
            .put("entityType", item.getEntityType())
            .put("entityId", item.getEntityId())
            .put("contentHash", item.getContentHash())
            .put("source", item.getSource());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType("AUDIT_EVENT_RECORDED")
            .tenantId(item.getTenantId())
            .businessId(item.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(item.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
