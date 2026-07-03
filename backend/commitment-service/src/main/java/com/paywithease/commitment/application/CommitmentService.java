package com.paywithease.commitment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.commitment.domain.Commitment;
import com.paywithease.commitment.domain.CommitmentEvent;
import com.paywithease.commitment.domain.CounterpartyType;
import com.paywithease.commitment.domain.SourceType;
import com.paywithease.commitment.infrastructure.CommitmentEventRepository;
import com.paywithease.commitment.infrastructure.CommitmentRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.idempotency.IdempotencyService;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Payment-promise lifecycle service with idempotent state changes and immutable event history. */
@Service
public class CommitmentService {

  private static final String SOURCE = "commitment-service";
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final CommitmentRepository commitments;
  private final CommitmentEventRepository events;
  private final IdempotencyService idempotency;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public CommitmentService(
      CommitmentRepository commitments,
      CommitmentEventRepository events,
      IdempotencyService idempotency,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.commitments = commitments;
    this.events = events;
    this.idempotency = idempotency;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public record CreateCommand(
      String counterpartyType,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      String description,
      long amountMinor,
      LocalDate dueDate) {}

  public record PaymentCommand(long amountMinor, String note) {}

  public record RescheduleCommand(LocalDate newDueDate, String note) {}

  public record CancelCommand(String note) {}

  public record CommitmentResult(
      String id,
      String counterpartyType,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      String description,
      long amountMinor,
      long paidMinor,
      long outstandingMinor,
      LocalDate dueDate,
      String status,
      Instant createdAt,
      Instant updatedAt,
      Instant closedAt) {
    public static CommitmentResult from(Commitment c) {
      return new CommitmentResult(
          c.getId(),
          c.getCounterpartyType(),
          c.getCounterpartyId(),
          c.getCounterpartyName(),
          c.getSourceType(),
          c.getSourceRef(),
          c.getDescription(),
          c.getAmountMinor(),
          c.getPaidMinor(),
          c.outstandingMinor(),
          c.getDueDate(),
          c.getStatus(),
          c.getCreatedAt(),
          c.getUpdatedAt(),
          c.getClosedAt());
    }
  }

  @Transactional
  public CommitmentResult create(String idempotencyKey, CreateCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    String requestHash = idempotency.hashRequest(cmd);
    return idempotency.execute(
        tenantId,
        idempotencyKey,
        "POST /commitments",
        requestHash,
        CommitmentResult.class,
        () -> CommitmentResult.from(doCreate(tenantId, cmd)));
  }

  private Commitment doCreate(String tenantId, CreateCommand cmd) {
    if (!CounterpartyType.isValid(cmd.counterpartyType())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown counterparty type");
    }
    String source = blankTo(cmd.sourceType(), SourceType.MANUAL.name());
    if (!SourceType.isValid(source)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown source type");
    }
    if (cmd.sourceRef() != null
        && commitments.existsByTenantIdAndSourceTypeAndSourceRef(
            tenantId, source, cmd.sourceRef())) {
      throw new ApiException(ErrorCode.CONFLICT, "A commitment already exists for this source");
    }

    Instant now = clock.instant();
    Commitment c =
        new Commitment(
            Ulid.newId(),
            tenantId,
            CounterpartyType.valueOf(cmd.counterpartyType()),
            cmd.counterpartyId(),
            cmd.counterpartyName(),
            SourceType.valueOf(source),
            cmd.sourceRef(),
            cmd.description(),
            cmd.amountMinor(),
            cmd.dueDate(),
            TenantContext.actorId().orElse(null),
            now);
    commitments.save(c);
    recordEvent(c, "COMMITMENT_CREATED", null, c.getDueDate(), null, cmd.description(), now);
    audit.record(
        "COMMITMENT_CREATED",
        "commitment",
        c.getId(),
        Map.of("amountMinor", c.getAmountMinor(), "dueDate", c.getDueDate().toString()));
    emit("COMMITMENT_CREATED", c, Map.of("dueDate", c.getDueDate().toString()));
    return c;
  }

  @Transactional
  public CommitmentResult recordPayment(String idempotencyKey, String id, PaymentCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    String requestHash = idempotency.hashRequest(new PaymentRequestHash(id, cmd));
    return idempotency.execute(
        tenantId,
        idempotencyKey,
        "POST /commitments/{id}/record-payment",
        requestHash,
        CommitmentResult.class,
        () -> CommitmentResult.from(doRecordPayment(id, cmd)));
  }

  private Commitment doRecordPayment(String id, PaymentCommand cmd) {
    Commitment c = getEntity(id);
    Instant now = clock.instant();
    long applied = c.recordPayment(cmd.amountMinor(), now);
    commitments.save(c);
    String eventType =
        "PAID".equals(c.getStatus()) ? "COMMITMENT_PAID" : "COMMITMENT_PARTIALLY_PAID";
    recordEvent(c, eventType, null, null, applied, cmd.note(), now);
    audit.record(
        eventType,
        "commitment",
        c.getId(),
        Map.of("appliedMinor", applied, "outstandingMinor", c.outstandingMinor()));
    emit(eventType, c, Map.of("appliedMinor", applied, "outstandingMinor", c.outstandingMinor()));
    return c;
  }

  @Transactional
  public CommitmentResult reschedule(String id, RescheduleCommand cmd) {
    Commitment c = getEntity(id);
    Instant now = clock.instant();
    LocalDate oldDueDate = c.reschedule(cmd.newDueDate(), cmd.note(), now);
    commitments.save(c);
    recordEvent(c, "COMMITMENT_RESCHEDULED", oldDueDate, c.getDueDate(), null, cmd.note(), now);
    audit.record(
        "COMMITMENT_RESCHEDULED",
        "commitment",
        c.getId(),
        Map.of("oldDueDate", oldDueDate.toString(), "newDueDate", c.getDueDate().toString()));
    emit(
        "COMMITMENT_RESCHEDULED",
        c,
        Map.of("oldDueDate", oldDueDate.toString(), "newDueDate", c.getDueDate().toString()));
    return CommitmentResult.from(c);
  }

  @Transactional
  public CommitmentResult cancel(String id, CancelCommand cmd) {
    Commitment c = getEntity(id);
    Instant now = clock.instant();
    c.cancel(cmd.note(), now);
    commitments.save(c);
    recordEvent(c, "COMMITMENT_CANCELLED", null, null, null, cmd.note(), now);
    audit.record("COMMITMENT_CANCELLED", "commitment", c.getId(), Map.of());
    emit("COMMITMENT_CANCELLED", c, Map.of("outstandingMinor", c.outstandingMinor()));
    return CommitmentResult.from(c);
  }

  @Transactional
  public int markOverdueBroken() {
    String tenantId = TenantContext.requireTenantId();
    LocalDate today = LocalDate.now(clock.withZone(IST));
    List<Commitment> overdue = commitments.overdue(tenantId, today);
    Instant now = clock.instant();
    int changed = 0;
    for (Commitment c : overdue) {
      if (!"BROKEN".equals(c.getStatus())) {
        c.markBroken(now);
        commitments.save(c);
        recordEvent(c, "COMMITMENT_BROKEN", null, c.getDueDate(), null, null, now);
        audit.record(
            "COMMITMENT_BROKEN",
            "commitment",
            c.getId(),
            Map.of("dueDate", c.getDueDate().toString(), "outstandingMinor", c.outstandingMinor()));
        emit("COMMITMENT_BROKEN", c, Map.of("outstandingMinor", c.outstandingMinor()));
        changed++;
      }
    }
    return changed;
  }

  @Transactional(readOnly = true)
  public CommitmentResult get(String id) {
    return CommitmentResult.from(getEntity(id));
  }

  @Transactional(readOnly = true)
  public List<CommitmentResult> list(String status, String counterpartyType) {
    return commitments
        .list(TenantContext.requireTenantId(), blankToNull(status), blankToNull(counterpartyType))
        .stream()
        .map(CommitmentResult::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CommitmentResult> dueSoon(int days) {
    LocalDate today = LocalDate.now(clock.withZone(IST));
    return commitments
        .dueSoon(TenantContext.requireTenantId(), today, today.plusDays(Math.max(0, days)))
        .stream()
        .map(CommitmentResult::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CommitmentResult> overdue() {
    LocalDate today = LocalDate.now(clock.withZone(IST));
    return commitments.overdue(TenantContext.requireTenantId(), today).stream()
        .map(CommitmentResult::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CommitmentEvent> eventsFor(String id) {
    getEntity(id); // tenant guard
    return events.findByCommitmentIdOrderByOccurredAtDesc(id);
  }

  private Commitment getEntity(String id) {
    return commitments
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Commitment"));
  }

  private void recordEvent(
      Commitment c,
      String eventType,
      LocalDate oldDueDate,
      LocalDate newDueDate,
      Long amountMinor,
      String note,
      Instant now) {
    events.save(
        new CommitmentEvent(
            Ulid.newId(),
            c.getTenantId(),
            c.getId(),
            eventType,
            oldDueDate,
            newDueDate,
            amountMinor,
            note,
            TenantContext.actorId().orElse(null),
            now));
  }

  private void emit(String eventType, Commitment c, Map<String, ?> extra) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("commitmentId", c.getId());
    payload.put("counterpartyType", c.getCounterpartyType());
    payload.put("counterpartyId", c.getCounterpartyId());
    payload.put("counterpartyName", c.getCounterpartyName());
    payload.put("sourceType", c.getSourceType());
    payload.put("sourceRef", c.getSourceRef());
    payload.put("amountMinor", c.getAmountMinor());
    payload.put("paidMinor", c.getPaidMinor());
    payload.put("outstandingMinor", c.outstandingMinor());
    payload.put("dueDate", c.getDueDate().toString());
    payload.put("status", c.getStatus());
    payload.setAll((ObjectNode) objectMapper.valueToTree(extra));
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(c.getTenantId())
            .businessId(c.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(c.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private record PaymentRequestHash(String id, PaymentCommand command) {}
}
