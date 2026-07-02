package com.paywithease.auditevidence.api;

import com.paywithease.auditevidence.domain.EvidenceItem;
import com.paywithease.auditevidence.domain.ExportJob;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Base64;

/** Request/response DTOs for the /audit API. */
public final class AuditEvidenceDtos {

  private AuditEvidenceDtos() {}

  /**
   * Record an uploaded artifact. The content is carried as Base64 (JSON convention, matching
   * sibling services) and its SHA-256 hash is computed by the service.
   */
  public record RecordEvidenceRequest(
      @NotBlank String entityType,
      @NotBlank String entityId,
      @NotBlank String contentBase64,
      String storageRef,
      String description) {}

  /** Re-verify a held artifact against the recorded hash for the given evidence item. */
  public record VerifyEvidenceRequest(@NotBlank String contentBase64) {}

  /** Request an auditor export for a scope (e.g. an entity type, a period, or "ALL"). */
  public record RequestExportRequest(@NotBlank String scope) {}

  /** Record the storage reference of a completed export result. */
  public record CompleteExportRequest(@NotBlank String resultRef) {}

  /** Record why an export failed. */
  public record FailExportRequest(@NotBlank String error) {}

  public record EvidenceResponse(
      String id,
      String entityType,
      String entityId,
      String contentHash,
      String storageRef,
      String description,
      String source,
      String uploadedBy,
      Instant createdAt) {}

  public record VerifyResponse(String evidenceId, boolean valid, String storedHash) {}

  public record ExportResponse(
      String id, String scope, String status, String resultRef, String error) {}

  /** Decodes Base64 content, translating a malformed value into a 400 rather than a 500. */
  static byte[] decode(String contentBase64) {
    try {
      return Base64.getDecoder().decode(contentBase64);
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "contentBase64 is not valid Base64");
    }
  }

  static EvidenceResponse toEvidence(EvidenceItem item) {
    return new EvidenceResponse(
        item.getId(),
        item.getEntityType(),
        item.getEntityId(),
        item.getContentHash(),
        item.getStorageRef(),
        item.getDescription(),
        item.getSource(),
        item.getUploadedBy(),
        item.getCreatedAt());
  }

  static ExportResponse toExport(ExportJob job) {
    return new ExportResponse(
        job.getId(), job.getScope(), job.getStatus(), job.getResultRef(), job.getError());
  }
}
