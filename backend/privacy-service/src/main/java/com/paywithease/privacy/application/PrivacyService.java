package com.paywithease.privacy.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.privacy.domain.DataCategory;
import com.paywithease.privacy.domain.DsrRequest;
import com.paywithease.privacy.domain.DsrType;
import com.paywithease.privacy.domain.ErasurePlan;
import com.paywithease.privacy.infrastructure.DsrRequestRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DPDP data-principal rights workflow. Requests are verified before any data is acted on; erasure
 * produces an explicit plan that retains (never hard-deletes) financial/tax/KYC records and
 * anonymises linked PII; downstream services act on the emitted {@code DATA_ERASURE_REQUESTED}
 * event. Every step is audited and tenant-scoped.
 */
@Service
public class PrivacyService {

  private static final String SOURCE = "privacy-service";

  private final DsrRequestRepository requests;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Duration sla;

  public PrivacyService(
      DsrRequestRepository requests,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.privacy.sla-days:30}") long slaDays) {
    this.requests = requests;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.sla = Duration.ofDays(slaDays);
  }

  @Transactional
  public DsrRequest submitRequest(
      DsrType type, String subjectRef, String subjectEmail, String details) {
    String tenantId = TenantContext.requireTenantId();
    DsrRequest request =
        new DsrRequest(
            Ulid.newId(),
            tenantId,
            type,
            subjectRef,
            subjectEmail,
            details,
            clock.instant(),
            clock.instant().plus(sla));
    requests.save(request);
    audit.record("DSR_RECEIVED", "dsr_request", request.getId(), Map.of("type", type.name()));
    emit("DSR_RECEIVED", request);
    if (type == DsrType.GRIEVANCE) {
      emit("DPDP_GRIEVANCE_RAISED", request);
    }
    return request;
  }

  @Transactional
  public DsrRequest startVerification(String id) {
    DsrRequest r = load(id);
    r.startVerification();
    requests.save(r);
    audit.record("DSR_VERIFICATION_STARTED", "dsr_request", id, Map.of());
    return r;
  }

  @Transactional
  public DsrRequest markVerified(String id) {
    DsrRequest r = load(id);
    r.markVerified(actor(), clock.instant());
    requests.save(r);
    audit.record("DSR_REQUESTER_VERIFIED", "dsr_request", id, Map.of());
    return r;
  }

  /**
   * Compute and attach the erasure plan for the given categories and emit {@code
   * DATA_ERASURE_REQUESTED} so each owning service anonymises/retains its slice. Financial/tax/KYC
   * data is retained under legal hold — the plan says so explicitly.
   */
  @Transactional
  public ErasurePlan.Plan planErasure(String id, List<DataCategory> categories) {
    DsrRequest r = load(id);
    ErasurePlan.Plan plan = ErasurePlan.build(categories);
    r.attachErasurePlan(writeJson(plan));
    requests.save(r);
    audit.record(
        "DATA_ERASURE_PLANNED",
        "dsr_request",
        id,
        Map.of("fullErasurePossible", plan.fullErasurePossible(), "summary", plan.summary()));
    ObjectNode payload = basePayload(r);
    payload.put("fullErasurePossible", plan.fullErasurePossible());
    payload.put("summary", plan.summary());
    emitPayload("DATA_ERASURE_REQUESTED", r, payload);
    return plan;
  }

  @Transactional
  public DsrRequest completeRequest(String id, String evidenceRef, String note) {
    DsrRequest r = load(id);
    r.complete(evidenceRef, note, clock.instant());
    requests.save(r);
    audit.record("DSR_COMPLETED", "dsr_request", id, Map.of());
    emit("DSR_COMPLETED", r);
    return r;
  }

  @Transactional
  public DsrRequest rejectRequest(String id, String reason) {
    DsrRequest r = load(id);
    r.reject(reason, clock.instant());
    requests.save(r);
    audit.record("DSR_REJECTED", "dsr_request", id, Map.of("reason", reason));
    return r;
  }

  @Transactional(readOnly = true)
  public DsrRequest get(String id) {
    return load(id);
  }

  @Transactional(readOnly = true)
  public List<DsrRequest> list(String status) {
    String tenantId = TenantContext.requireTenantId();
    return status == null || status.isBlank()
        ? requests.findByTenantIdOrderByReceivedAtDesc(tenantId)
        : requests.findByTenantIdAndStatusOrderByReceivedAtDesc(tenantId, status);
  }

  @Transactional(readOnly = true)
  public List<DsrRequest> overdue() {
    return requests.findByTenantIdOrderByReceivedAtDesc(TenantContext.requireTenantId()).stream()
        .filter(r -> r.isOverdue(clock.instant()))
        .toList();
  }

  private DsrRequest load(String id) {
    return requests
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Data subject request"));
  }

  private String actor() {
    return TenantContext.actorId()
        .orElseThrow(
            () -> new ApiException(ErrorCode.UNAUTHENTICATED, "No acting user in context"));
  }

  private ObjectNode basePayload(DsrRequest r) {
    return objectMapper
        .createObjectNode()
        .put("requestId", r.getId())
        .put("type", r.getType())
        .put("status", r.getStatus());
  }

  private void emit(String eventType, DsrRequest r) {
    emitPayload(eventType, r, basePayload(r));
  }

  private void emitPayload(String eventType, DsrRequest r, ObjectNode payload) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(r.getTenantId())
            .businessId(r.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(r.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      return "{}";
    }
  }
}
