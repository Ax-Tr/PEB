package com.paywithease.commitment.api;

import com.paywithease.commitment.application.CommitmentService;
import com.paywithease.commitment.domain.CommitmentEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Payment commitments with due dates, partial payment tracking, and broken-promise visibility. */
@RestController
@RequestMapping("/api/v1/commitments")
@Tag(name = "commitments", description = "Payment promises and due-date tracking")
public class CommitmentController {

  private final CommitmentService service;

  public CommitmentController(CommitmentService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a payment commitment")
  public CommitmentDtos.CommitmentResponse create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CommitmentDtos.CreateCommitmentRequest body) {
    return toResponse(
        service.create(
            idempotencyKey,
            new CommitmentService.CreateCommand(
                body.counterpartyType(),
                body.counterpartyId(),
                body.counterpartyName(),
                body.sourceType(),
                body.sourceRef(),
                body.description(),
                body.amountMinor(),
                body.dueDate())));
  }

  @GetMapping
  @Operation(summary = "List commitments with optional status/counterparty filters")
  public List<CommitmentDtos.CommitmentResponse> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String counterpartyType) {
    return service.list(status, counterpartyType).stream()
        .map(CommitmentController::toResponse)
        .toList();
  }

  @GetMapping("/due-soon")
  public List<CommitmentDtos.CommitmentResponse> dueSoon(
      @RequestParam(defaultValue = "7") int days) {
    return service.dueSoon(days).stream().map(CommitmentController::toResponse).toList();
  }

  @GetMapping("/overdue")
  public List<CommitmentDtos.CommitmentResponse> overdue() {
    return service.overdue().stream().map(CommitmentController::toResponse).toList();
  }

  @PostMapping("/mark-overdue-broken")
  @Operation(summary = "Mark currently overdue open commitments as BROKEN")
  public CommitmentDtos.BrokenMarkResponse markOverdueBroken() {
    return new CommitmentDtos.BrokenMarkResponse(service.markOverdueBroken());
  }

  @GetMapping("/{id}")
  public CommitmentDtos.CommitmentDetailResponse get(@PathVariable String id) {
    return new CommitmentDtos.CommitmentDetailResponse(
        toResponse(service.get(id)),
        service.eventsFor(id).stream().map(CommitmentController::toEvent).toList());
  }

  @PostMapping("/{id}/record-payment")
  @Operation(summary = "Record a payment against a commitment")
  public CommitmentDtos.CommitmentResponse recordPayment(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @PathVariable String id,
      @Valid @RequestBody CommitmentDtos.RecordPaymentRequest body) {
    return toResponse(
        service.recordPayment(
            idempotencyKey,
            id,
            new CommitmentService.PaymentCommand(body.amountMinor(), body.note())));
  }

  @PostMapping("/{id}/reschedule")
  public CommitmentDtos.CommitmentResponse reschedule(
      @PathVariable String id, @Valid @RequestBody CommitmentDtos.RescheduleRequest body) {
    return toResponse(
        service.reschedule(
            id, new CommitmentService.RescheduleCommand(body.newDueDate(), body.note())));
  }

  @PostMapping("/{id}/cancel")
  public CommitmentDtos.CommitmentResponse cancel(
      @PathVariable String id, @RequestBody(required = false) CommitmentDtos.CancelRequest body) {
    return toResponse(
        service.cancel(id, new CommitmentService.CancelCommand(body == null ? null : body.note())));
  }

  private static CommitmentDtos.CommitmentResponse toResponse(
      CommitmentService.CommitmentResult c) {
    return new CommitmentDtos.CommitmentResponse(
        c.id(),
        c.counterpartyType(),
        c.counterpartyId(),
        c.counterpartyName(),
        c.sourceType(),
        c.sourceRef(),
        c.description(),
        c.amountMinor(),
        c.paidMinor(),
        c.outstandingMinor(),
        c.dueDate(),
        c.status(),
        c.createdAt(),
        c.updatedAt(),
        c.closedAt());
  }

  private static CommitmentDtos.CommitmentEventResponse toEvent(CommitmentEvent e) {
    return new CommitmentDtos.CommitmentEventResponse(
        e.getId(),
        e.getEventType(),
        e.getOldDueDate(),
        e.getNewDueDate(),
        e.getAmountMinor(),
        e.getNote(),
        e.getOccurredAt());
  }
}
