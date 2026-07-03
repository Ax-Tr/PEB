package com.paywithease.ocr.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ocr.domain.DocumentRecord;
import com.paywithease.ocr.domain.DocumentType;
import com.paywithease.ocr.domain.OcrJob;
import com.paywithease.ocr.domain.OcrJobStatus;
import com.paywithease.ocr.infrastructure.DocumentRecordRepository;
import com.paywithease.ocr.infrastructure.OcrJobRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcrDocumentService {

  private static final String SOURCE = "ocr-document-service";
  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "application/pdf");

  private final DocumentRecordRepository documents;
  private final OcrJobRepository jobs;
  private final OcrProvider ocrProvider;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final long maxSizeBytes;
  private final long uploadTtlMinutes;

  public OcrDocumentService(
      DocumentRecordRepository documents,
      OcrJobRepository jobs,
      OcrProvider ocrProvider,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.ocr.max-size-bytes:10485760}") long maxSizeBytes,
      @Value("${peb.ocr.upload-url-ttl-minutes:15}") long uploadTtlMinutes) {
    this.documents = documents;
    this.jobs = jobs;
    this.ocrProvider = ocrProvider;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.maxSizeBytes = maxSizeBytes;
    this.uploadTtlMinutes = uploadTtlMinutes;
  }

  public record UploadCommand(String filename, String mimeType, String checksum, long sizeBytes) {}

  public record UploadReservation(
      String documentId,
      String storageKey,
      String uploadUrl,
      Instant expiresAt,
      DocumentResponse document) {}

  public record StartJobCommand(String documentId, String documentType, String rawText) {}

  public record ReviewCommand(boolean accepted, Map<String, Object> fields) {}

  public record DocumentResponse(
      String id,
      String storageKey,
      String originalFilename,
      String mimeType,
      String checksum,
      long sizeBytes,
      Instant createdAt) {
    static DocumentResponse from(DocumentRecord d) {
      return new DocumentResponse(
          d.getId(),
          d.getStorageKey(),
          d.getOriginalFilename(),
          d.getMimeType(),
          d.getChecksum(),
          d.getSizeBytes(),
          d.getCreatedAt());
    }
  }

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

  @Transactional
  public UploadReservation reserveUpload(UploadCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    validateUpload(cmd);
    Instant now = clock.instant();
    String documentId = Ulid.newId();
    String storageKey = tenantId + "/" + documentId + "/" + sanitizeFilename(cmd.filename());
    DocumentRecord document =
        new DocumentRecord(
            documentId,
            tenantId,
            storageKey,
            cmd.filename(),
            cmd.mimeType(),
            cmd.checksum(),
            cmd.sizeBytes(),
            TenantContext.actorId().orElse(null),
            now);
    documents.save(document);
    audit.record(
        "OCR_DOCUMENT_RESERVED",
        "document",
        documentId,
        Map.of("mimeType", cmd.mimeType(), "sizeBytes", cmd.sizeBytes()));
    return new UploadReservation(
        documentId,
        storageKey,
        "peb-local-upload://" + storageKey,
        now.plus(uploadTtlMinutes, ChronoUnit.MINUTES),
        DocumentResponse.from(document));
  }

  @Transactional
  public OcrJobResponse startJob(StartJobCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    if (!DocumentType.isValid(cmd.documentType())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown OCR document type");
    }
    DocumentRecord document =
        documents
            .findByTenantIdAndId(tenantId, cmd.documentId())
            .orElseThrow(() -> ApiException.notFound("Document"));
    OcrJob job =
        new OcrJob(
            Ulid.newId(),
            tenantId,
            document.getId(),
            DocumentType.valueOf(cmd.documentType()),
            clock.instant());
    jobs.save(job);

    try {
      OcrProvider.OcrExtraction extraction =
          ocrProvider.extract(document, DocumentType.valueOf(cmd.documentType()), cmd.rawText());
      job.applyExtraction(
          extraction.rawText(),
          objectMapper.writeValueAsString(extraction.fields()),
          extraction.confidence(),
          clock.instant());
      audit.record(
          "OCR_REVIEW_REQUIRED",
          "ocr_job",
          job.getId(),
          Map.of("documentId", document.getId(), "confidence", job.getConfidence()));
      emit("OCR_REVIEW_REQUIRED", job);
    } catch (JsonProcessingException e) {
      job.fail("OCR extraction failed", clock.instant());
      audit.record("OCR_FAILED", "ocr_job", job.getId(), Map.of("documentId", document.getId()));
    }
    return toResponse(jobs.save(job));
  }

  @Transactional(readOnly = true)
  public OcrJobResponse getJob(String id) {
    return toResponse(getJobEntity(id));
  }

  @Transactional(readOnly = true)
  public List<OcrJobResponse> listJobs() {
    return jobs.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public OcrJobResponse review(String id, ReviewCommand cmd) {
    OcrJob job = getJobEntity(id);
    if (cmd.accepted() && OcrJobStatus.COMPLETED.name().equals(job.getStatus())) {
      return toResponse(job);
    }
    if (!cmd.accepted() && OcrJobStatus.FAILED.name().equals(job.getStatus())) {
      return toResponse(job);
    }
    if (!OcrJobStatus.REVIEW_REQUIRED.name().equals(job.getStatus())) {
      throw new ApiException(ErrorCode.CONFLICT, "Only OCR jobs pending review can be reviewed");
    }
    Map<String, Object> reviewedFields =
        cmd.fields() == null ? readFields(job.getExtractedFieldsJson()) : cmd.fields();
    validateReviewAcceptance(job, cmd.accepted(), reviewedFields);
    try {
      job.review(
          cmd.accepted(),
          objectMapper.writeValueAsString(reviewedFields),
          TenantContext.actorId().orElse(null),
          clock.instant());
    } catch (JsonProcessingException e) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid reviewed OCR fields");
    }
    jobs.save(job);
    audit.record(
        cmd.accepted() ? "OCR_ACCEPTED" : "OCR_REJECTED",
        "ocr_job",
        job.getId(),
        Map.of("documentId", job.getDocumentId()));
    emit(cmd.accepted() ? "OCR_ACCEPTED" : "OCR_REJECTED", job);
    return toResponse(job);
  }

  private OcrJob getJobEntity(String id) {
    return jobs.findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("OCR job"));
  }

  private OcrJobResponse toResponse(OcrJob job) {
    return new OcrJobResponse(
        job.getId(),
        job.getDocumentId(),
        job.getDocumentType(),
        job.getStatus(),
        readFields(job.getExtractedFieldsJson()),
        job.getConfidence(),
        job.getFailureReason(),
        job.getCreatedAt(),
        job.getUpdatedAt(),
        job.getReviewedAt(),
        job.getReviewedBy());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readFields(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }

  private void validateReviewAcceptance(
      OcrJob job, boolean accepted, Map<String, Object> reviewedFields) {
    if (!accepted || !requiresBankFields(job.getDocumentType())) {
      return;
    }
    requirePresent(reviewedFields, "accountNumber");
    requirePresent(reviewedFields, "ifsc");
    requirePresent(reviewedFields, "bankName");
    requirePresent(reviewedFields, "holderName");

    String normalizedAccount =
        reviewedValue(reviewedFields, "accountNumber").replaceAll("[\\s-]", "");
    if (!normalizedAccount.matches("\\d{6,18}")) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "OCR bank detail accountNumber is invalid");
    }
    String ifsc = reviewedValue(reviewedFields, "ifsc").trim().toUpperCase();
    if (!ifsc.matches("[A-Z]{4}0[A-Z0-9]{6}")) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "OCR bank detail IFSC is invalid");
    }
  }

  private static void requirePresent(Map<String, Object> fields, String key) {
    if (reviewedValue(fields, key).isBlank()) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "OCR bank detail is missing required field: " + key);
    }
  }

  private static boolean requiresBankFields(String documentType) {
    return DocumentType.BANK_DETAILS.name().equals(documentType)
        || DocumentType.CHEQUE.name().equals(documentType)
        || DocumentType.PASSBOOK.name().equals(documentType);
  }

  private static String reviewedValue(Map<String, Object> fields, String key) {
    if (fields == null || !fields.containsKey(key)) {
      return "";
    }
    Object value = fields.get(key);
    if (value instanceof Map<?, ?> nested) {
      Object nestedValue = nested.get("value");
      return nestedValue == null ? "" : nestedValue.toString().trim();
    }
    return value == null ? "" : value.toString().trim();
  }

  private void validateUpload(UploadCommand cmd) {
    if (cmd.filename() == null || cmd.filename().isBlank()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "filename is required");
    }
    if (!ALLOWED_MIME_TYPES.contains(cmd.mimeType())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unsupported OCR document type");
    }
    if (cmd.sizeBytes() <= 0 || cmd.sizeBytes() > maxSizeBytes) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "OCR document size exceeds limit");
    }
  }

  private static String sanitizeFilename(String filename) {
    return filename.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private void emit(String eventType, OcrJob job) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("ocrJobId", job.getId());
    payload.put("documentId", job.getDocumentId());
    payload.put("documentType", job.getDocumentType());
    payload.put("status", job.getStatus());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(job.getTenantId())
            .businessId(job.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(job.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
