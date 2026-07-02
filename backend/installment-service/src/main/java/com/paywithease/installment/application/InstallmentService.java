package com.paywithease.installment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.installment.domain.EmiScheduleGenerator;
import com.paywithease.installment.domain.Frequency;
import com.paywithease.installment.domain.Installment;
import com.paywithease.installment.domain.InstallmentEmi;
import com.paywithease.installment.domain.InstallmentType;
import com.paywithease.installment.infrastructure.InstallmentEmiRepository;
import com.paywithease.installment.infrastructure.InstallmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receivable/payable EMI schedules: generation (exact paise split), per-EMI payment application
 * with balance tracking and auto-closure, and modification with an audit trail. Cash movement is
 * booked by the payment/payout services — this service tracks the schedule only (no ledger posting
 * here).
 */
@Service
public class InstallmentService {

  private static final String SOURCE = "installment-service";

  private final InstallmentRepository installments;
  private final InstallmentEmiRepository emis;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public InstallmentService(
      InstallmentRepository installments,
      InstallmentEmiRepository emis,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.installments = installments;
    this.emis = emis;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public record CreateCommand(
      String type,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      long totalAmountMinor,
      int numberOfEmis,
      LocalDate firstDueDate,
      String frequency) {}

  @Transactional
  public Installment createSchedule(CreateCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    if (!InstallmentType.isValid(cmd.type())) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "Unknown installment type: " + cmd.type());
    }
    // Idempotent per source document: one schedule per invoice/bill.
    if (cmd.sourceRef() != null
        && installments.existsByTenantIdAndSourceRef(tenantId, cmd.sourceRef())) {
      throw new ApiException(
          ErrorCode.CONFLICT, "A schedule already exists for this source document");
    }

    Instant now = clock.instant();
    Frequency frequency = Frequency.of(cmd.frequency());
    Installment installment =
        new Installment(
            Ulid.newId(),
            tenantId,
            InstallmentType.valueOf(cmd.type()),
            cmd.counterpartyId(),
            cmd.counterpartyName(),
            cmd.sourceType(),
            cmd.sourceRef(),
            cmd.totalAmountMinor(),
            cmd.numberOfEmis(),
            frequency,
            now);
    installments.save(installment);

    for (EmiScheduleGenerator.PlannedEmi p :
        EmiScheduleGenerator.generate(
            cmd.totalAmountMinor(), cmd.numberOfEmis(), cmd.firstDueDate(), frequency)) {
      emis.save(
          new InstallmentEmi(
              Ulid.newId(),
              tenantId,
              installment.getId(),
              p.emiNumber(),
              p.dueDate(),
              p.amountMinor()));
    }

    audit.record(
        "INSTALLMENT_SCHEDULE_CREATED",
        "installment",
        installment.getId(),
        Map.of(
            "type",
            cmd.type(),
            "totalAmountMinor",
            cmd.totalAmountMinor(),
            "emis",
            cmd.numberOfEmis()));
    emit("INSTALLMENT_SCHEDULE_CREATED", installment, Map.of("numberOfEmis", cmd.numberOfEmis()));
    return installment;
  }

  @Transactional
  public Installment payEmi(String installmentId, int emiNumber, long amountMinor) {
    if (amountMinor <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "amount must be positive");
    }
    Installment installment = loadActive(installmentId);
    Instant now = clock.instant();
    InstallmentEmi emi =
        emis.findByInstallmentIdOrderByEmiNumber(installmentId).stream()
            .filter(e -> e.getEmiNumber() == emiNumber)
            .findFirst()
            .orElseThrow(() -> ApiException.notFound("EMI"));

    long applied = emi.apply(amountMinor, now);
    if (applied == 0) {
      throw new ApiException(ErrorCode.CONFLICT, "EMI already fully paid");
    }
    emis.save(emi);
    installment.reduceOutstanding(applied, now);
    installments.save(installment);

    audit.record(
        "INSTALLMENT_PAID",
        "installment",
        installmentId,
        Map.of("emiNumber", emiNumber, "appliedMinor", applied));
    emit(
        "INSTALLMENT_PAID",
        installment,
        Map.of(
            "emiNumber",
            emiNumber,
            "appliedMinor",
            applied,
            "outstandingMinor",
            installment.getOutstandingMinor(),
            "closed",
            !installment.isActive()));
    return installment;
  }

  /**
   * Reschedules the remaining balance into a new set of EMIs (paid EMIs are preserved). Rejected if
   * any EMI is partially paid — settle it first — so per-EMI history stays consistent.
   */
  @Transactional
  public Installment modifySchedule(
      String installmentId, int newNumberOfEmis, LocalDate newFirstDueDate, String frequencyRaw) {
    Installment installment = loadActive(installmentId);
    Instant now = clock.instant();
    List<InstallmentEmi> existing = emis.findByInstallmentIdOrderByEmiNumber(installmentId);

    long paidCount = existing.stream().filter(InstallmentEmi::isPaid).count();
    boolean anyPartial = existing.stream().anyMatch(e -> "PARTIAL".equals(e.getStatus()));
    if (anyPartial) {
      throw new ApiException(ErrorCode.CONFLICT, "Settle the partially-paid EMI before modifying");
    }

    // Remove the unpaid (PENDING) EMIs and regenerate for the outstanding balance.
    existing.stream().filter(e -> !e.isPaid()).forEach(emis::delete);
    Frequency frequency = Frequency.of(frequencyRaw);
    int startNumber = (int) paidCount + 1;
    int n = 1;
    for (EmiScheduleGenerator.PlannedEmi p :
        EmiScheduleGenerator.generate(
            installment.getOutstandingMinor(), newNumberOfEmis, newFirstDueDate, frequency)) {
      emis.save(
          new InstallmentEmi(
              Ulid.newId(),
              installment.getTenantId(),
              installmentId,
              startNumber + (n++ - 1),
              p.dueDate(),
              p.amountMinor()));
    }
    installment.touch(now);
    installments.save(installment);

    audit.record(
        "INSTALLMENT_MODIFIED",
        "installment",
        installmentId,
        Map.of(
            "newNumberOfEmis",
            newNumberOfEmis,
            "outstandingMinor",
            installment.getOutstandingMinor()));
    return installment;
  }

  @Transactional
  public Installment cancel(String installmentId) {
    Installment installment = loadActive(installmentId);
    installment.cancel(clock.instant());
    installments.save(installment);
    audit.record("INSTALLMENT_CANCELLED", "installment", installmentId, Map.of());
    return installment;
  }

  @Transactional(readOnly = true)
  public Installment get(String id) {
    return installments
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Installment"));
  }

  @Transactional(readOnly = true)
  public List<InstallmentEmi> emisFor(String installmentId) {
    get(installmentId); // tenant guard
    return emis.findByInstallmentIdOrderByEmiNumber(installmentId);
  }

  @Transactional(readOnly = true)
  public List<Installment> listByType(String type) {
    return installments.findByTenantIdAndTypeOrderByCreatedAtDesc(
        TenantContext.requireTenantId(), type);
  }

  private Installment loadActive(String id) {
    Installment installment = get(id);
    if (!installment.isActive()) {
      throw new ApiException(ErrorCode.CONFLICT, "Schedule is " + installment.getStatus());
    }
    return installment;
  }

  private void emit(String eventType, Installment installment, Map<String, ?> extra) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("installmentId", installment.getId());
    payload.put("type", installment.getType());
    payload.put("counterpartyId", installment.getCounterpartyId());
    payload.put("outstandingMinor", installment.getOutstandingMinor());
    payload.setAll((ObjectNode) objectMapper.valueToTree(extra));
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(installment.getTenantId())
            .businessId(installment.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(installment.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
