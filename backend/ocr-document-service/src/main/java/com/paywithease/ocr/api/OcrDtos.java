package com.paywithease.ocr.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public final class OcrDtos {

  private OcrDtos() {}

  public record UploadReservationRequest(
      @NotBlank String filename,
      @NotBlank String mimeType,
      String checksum,
      @Positive long sizeBytes) {}

  public record DocumentResponse(
      String id,
      String storageKey,
      String originalFilename,
      String mimeType,
      String checksum,
      long sizeBytes,
      Instant createdAt) {}

  public record UploadReservationResponse(
      String documentId,
      String storageKey,
      String uploadUrl,
      Instant expiresAt,
      DocumentResponse document) {}

  public record StartOcrJobRequest(
      @NotBlank String documentId, @NotBlank String documentType, String rawText) {}

  public record ReviewOcrJobRequest(boolean accepted, Map<String, Object> fields) {}

  public record OcrJobResponse(
      String id,
      String documentId,
      String documentType,
      String status,
      Map<String, Object> fields,
      BigDecimal confidence,
      String failureReason,
      Instant createdAt,
      Instant updatedAt,
      Instant reviewedAt,
      String reviewedBy) {}
}
