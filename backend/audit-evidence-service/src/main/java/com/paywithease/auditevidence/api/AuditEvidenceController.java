package com.paywithease.auditevidence.api;

import com.paywithease.auditevidence.application.AuditEvidenceService;
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
 * Audit evidence API — record and read immutable evidence, verify integrity against the stored
 * SHA-256 hash, and drive auditor export jobs.
 *
 * <p>Evidence is append-only: there is deliberately NO update or delete endpoint (the domain and a
 * database trigger enforce the same). Recording evidence and the export lifecycle require a finance
 * authority; read and verify are open to any authenticated principal so an auditor can inspect and
 * confirm evidence with a read-only token.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(
    name = "audit-evidence",
    description = "Immutable, append-only evidence room with SHA-256 integrity verification")
public class AuditEvidenceController {

  private final AuditEvidenceService service;

  public AuditEvidenceController(AuditEvidenceService service) {
    this.service = service;
  }

  // -------- Evidence (append-only: no PUT/PATCH/DELETE) --------

  @PostMapping("/evidence")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Record uploaded evidence (SHA-256 computed from the supplied content)")
  public AuditEvidenceDtos.EvidenceResponse record(
      @Valid @RequestBody AuditEvidenceDtos.RecordEvidenceRequest body) {
    byte[] content = AuditEvidenceDtos.decode(body.contentBase64());
    return AuditEvidenceDtos.toEvidence(
        service.recordUploadedEvidence(
            body.entityType(), body.entityId(), content, body.storageRef(), body.description()));
  }

  @GetMapping("/evidence")
  @Operation(summary = "List evidence recorded for an entity (newest first)")
  public List<AuditEvidenceDtos.EvidenceResponse> list(
      @RequestParam String entityType, @RequestParam String entityId) {
    return service.listEvidence(entityType, entityId).stream()
        .map(AuditEvidenceDtos::toEvidence)
        .toList();
  }

  @GetMapping("/evidence/{id}")
  @Operation(summary = "Get a single evidence item")
  public AuditEvidenceDtos.EvidenceResponse get(@PathVariable String id) {
    return AuditEvidenceDtos.toEvidence(service.getEvidence(id));
  }

  @PostMapping("/evidence/{id}/verify")
  @Operation(summary = "Verify a held artifact matches the recorded SHA-256 hash")
  public AuditEvidenceDtos.VerifyResponse verify(
      @PathVariable String id, @Valid @RequestBody AuditEvidenceDtos.VerifyEvidenceRequest body) {
    byte[] content = AuditEvidenceDtos.decode(body.contentBase64());
    AuditEvidenceService.VerifyResult r = service.verifyIntegrity(id, content);
    return new AuditEvidenceDtos.VerifyResponse(r.evidenceId(), r.valid(), r.storedHash());
  }

  // -------- Export jobs --------

  @PostMapping("/exports")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Request an auditor export job")
  public AuditEvidenceDtos.ExportResponse requestExport(
      @Valid @RequestBody AuditEvidenceDtos.RequestExportRequest body) {
    return AuditEvidenceDtos.toExport(service.requestExport(body.scope()));
  }

  @GetMapping("/exports")
  @Operation(summary = "List export jobs for the tenant (newest first)")
  public List<AuditEvidenceDtos.ExportResponse> listExports() {
    return service.listExports().stream().map(AuditEvidenceDtos::toExport).toList();
  }

  @PostMapping("/exports/{id}/start")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Move a requested export to PROCESSING")
  public AuditEvidenceDtos.ExportResponse startExport(@PathVariable String id) {
    return AuditEvidenceDtos.toExport(service.startExport(id));
  }

  @PostMapping("/exports/{id}/complete")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Mark a processing export COMPLETED with its result reference")
  public AuditEvidenceDtos.ExportResponse completeExport(
      @PathVariable String id, @Valid @RequestBody AuditEvidenceDtos.CompleteExportRequest body) {
    return AuditEvidenceDtos.toExport(service.completeExport(id, body.resultRef()));
  }

  @PostMapping("/exports/{id}/fail")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Mark an export FAILED with a reason")
  public AuditEvidenceDtos.ExportResponse failExport(
      @PathVariable String id, @Valid @RequestBody AuditEvidenceDtos.FailExportRequest body) {
    return AuditEvidenceDtos.toExport(service.failExport(id, body.error()));
  }
}
