package com.paywithease.installment.api;

import com.paywithease.installment.application.InstallmentService;
import com.paywithease.installment.domain.Installment;
import com.paywithease.installment.domain.InstallmentEmi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Receivable/payable EMI schedules. */
@RestController
@RequestMapping("/api/v1/installments")
@Tag(name = "installments", description = "Receivable/payable EMI schedules")
public class InstallmentController {

  private final InstallmentService service;

  public InstallmentController(InstallmentService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an EMI schedule")
  public InstallmentDtos.InstallmentResponse create(
      @Valid @RequestBody InstallmentDtos.CreateScheduleRequest body) {
    Installment inst =
        service.createSchedule(
            new InstallmentService.CreateCommand(
                body.type(),
                body.counterpartyId(),
                body.counterpartyName(),
                body.sourceType(),
                body.sourceRef(),
                body.totalAmountMinor(),
                body.numberOfEmis(),
                body.firstDueDate(),
                body.frequency()));
    return withEmis(inst);
  }

  @GetMapping
  @Operation(summary = "List schedules by type (RECEIVABLE|PAYABLE)")
  public List<InstallmentDtos.InstallmentResponse> list(@RequestParam String type) {
    return service.listByType(type).stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  public InstallmentDtos.InstallmentResponse get(@PathVariable String id) {
    return withEmis(service.get(id));
  }

  @PostMapping("/{id}/pay")
  @Operation(summary = "Record a payment against a specific EMI")
  public InstallmentDtos.InstallmentResponse pay(
      @PathVariable String id, @Valid @RequestBody InstallmentDtos.PayEmiRequest body) {
    service.payEmi(id, body.emiNumber(), body.amountMinor());
    return withEmis(service.get(id));
  }

  @PostMapping("/{id}/modify")
  @Operation(summary = "Reschedule the remaining balance (audited)")
  public InstallmentDtos.InstallmentResponse modify(
      @PathVariable String id, @Valid @RequestBody InstallmentDtos.ModifyRequest body) {
    service.modifySchedule(id, body.numberOfEmis(), body.firstDueDate(), body.frequency());
    return withEmis(service.get(id));
  }

  @PostMapping("/{id}/cancel")
  public InstallmentDtos.InstallmentResponse cancel(@PathVariable String id) {
    return withEmis(service.cancel(id));
  }

  private InstallmentDtos.InstallmentResponse withEmis(Installment inst) {
    List<InstallmentDtos.EmiResponse> emiRows =
        service.emisFor(inst.getId()).stream().map(InstallmentController::toEmi).toList();
    return toResponse(inst, emiRows);
  }

  private InstallmentDtos.InstallmentResponse toResponse(Installment inst) {
    return toResponse(inst, List.of());
  }

  private InstallmentDtos.InstallmentResponse toResponse(
      Installment i, List<InstallmentDtos.EmiResponse> emiRows) {
    return new InstallmentDtos.InstallmentResponse(
        i.getId(),
        i.getType(),
        i.getCounterpartyId(),
        i.getCounterpartyName(),
        i.getSourceType(),
        i.getSourceRef(),
        i.getTotalAmountMinor(),
        i.getOutstandingMinor(),
        i.getNumberOfEmis(),
        i.getFrequency(),
        i.getStatus(),
        emiRows);
  }

  private static InstallmentDtos.EmiResponse toEmi(InstallmentEmi e) {
    return new InstallmentDtos.EmiResponse(
        e.getId(),
        e.getEmiNumber(),
        e.getDueDate(),
        e.getAmountMinor(),
        e.getPaidMinor(),
        e.getStatus());
  }
}
