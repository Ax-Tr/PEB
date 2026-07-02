package com.paywithease.privacy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.privacy.application.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DPDP data-principal rights API — intake and workflow for access/correction/erasure/portability/
 * grievance requests.
 *
 * <p>The requester's identity is verified before any data is acted on (the state machine forces
 * {@code verify} before an erasure plan or completion). Erasure never hard-deletes
 * financial/tax/KYC records — the erasure plan surfaces {@code fullErasurePossible=false} honestly
 * and downstream services perform the actual per-service anonymisation/retention in response to the
 * emitted {@code DATA_ERASURE_REQUESTED} event. The subject's email is PII: encrypted at rest and
 * never logged.
 *
 * <p>All endpoints are restricted to the Data Protection Officer function (mapped to
 * OWNER/CO_OWNER/SUPPORT_ADMIN) because DSR data includes the requester's email PII.
 */
@RestController
@RequestMapping("/api/v1/privacy")
@Tag(
    name = "privacy",
    description = "DPDP data-principal rights (access/correction/erasure/portability/grievance)")
public class PrivacyController {

  private final PrivacyService service;
  private final ObjectMapper objectMapper;

  public PrivacyController(PrivacyService service, ObjectMapper objectMapper) {
    this.service = service;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/requests")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(summary = "Intake a data-principal request (identity is verified in a later step)")
  public PrivacyDtos.DsrResponse submit(@Valid @RequestBody PrivacyDtos.SubmitRequest body) {
    return PrivacyDtos.toDsr(
        service.submitRequest(
            PrivacyDtos.parseType(body.type()),
            body.subjectRef(),
            body.subjectEmail(),
            body.details()),
        objectMapper);
  }

  @PostMapping("/requests/{id}/start-verification")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(summary = "Begin verifying the requester's identity")
  public PrivacyDtos.DsrResponse startVerification(@PathVariable String id) {
    return PrivacyDtos.toDsr(service.startVerification(id), objectMapper);
  }

  @PostMapping("/requests/{id}/verify")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(
      summary =
          "Record that the requester's identity was verified (represents the email/OTP proof"
              + " integration). No data is acted on before this step.")
  public PrivacyDtos.DsrResponse verify(@PathVariable String id) {
    return PrivacyDtos.toDsr(service.markVerified(id), objectMapper);
  }

  @PostMapping("/requests/{id}/erasure-plan")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(
      summary =
          "Compute the erasure plan for the given categories. Financial/tax/KYC records are never"
              + " hard-deleted — retained under legal hold with linked PII anonymised.")
  public PrivacyDtos.ErasurePlanResponse erasurePlan(
      @PathVariable String id, @Valid @RequestBody PrivacyDtos.ErasurePlanRequest body) {
    return PrivacyDtos.toPlan(
        service.planErasure(id, PrivacyDtos.parseCategories(body.categories())));
  }

  @PostMapping("/requests/{id}/complete")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(summary = "Complete a verified, in-progress request with resolution evidence")
  public PrivacyDtos.DsrResponse complete(
      @PathVariable String id, @Valid @RequestBody PrivacyDtos.CompleteRequest body) {
    return PrivacyDtos.toDsr(
        service.completeRequest(id, body.evidenceRef(), body.note()), objectMapper);
  }

  @PostMapping("/requests/{id}/reject")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(summary = "Reject a request with a reason")
  public PrivacyDtos.DsrResponse reject(
      @PathVariable String id, @Valid @RequestBody PrivacyDtos.RejectRequest body) {
    return PrivacyDtos.toDsr(service.rejectRequest(id, body.reason()), objectMapper);
  }

  @GetMapping("/requests")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(summary = "List DSRs for the tenant (optionally filtered by status; newest first)")
  public List<PrivacyDtos.DsrResponse> list(@RequestParam(required = false) String status) {
    return service.list(status).stream().map(r -> PrivacyDtos.toDsr(r, objectMapper)).toList();
  }

  @GetMapping("/requests/overdue")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(summary = "List DSRs past their SLA due date (SLA breach monitoring)")
  public List<PrivacyDtos.DsrResponse> overdue() {
    return service.overdue().stream().map(r -> PrivacyDtos.toDsr(r, objectMapper)).toList();
  }

  @GetMapping("/requests/{id}")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','SUPPORT_ADMIN')")
  @Operation(summary = "Get a DSR")
  public PrivacyDtos.DsrResponse get(@PathVariable String id) {
    return PrivacyDtos.toDsr(service.get(id), objectMapper);
  }
}
