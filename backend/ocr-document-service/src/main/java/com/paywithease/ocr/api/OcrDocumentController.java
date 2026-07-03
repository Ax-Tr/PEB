package com.paywithease.ocr.api;

import com.paywithease.ocr.application.OcrDocumentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "ocr-documents", description = "Secure OCR capture and review workflow")
public class OcrDocumentController {

  private final OcrDocumentService service;

  public OcrDocumentController(OcrDocumentService service) {
    this.service = service;
  }

  @PostMapping("/documents/upload-url")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Reserve a secure upload slot for a document")
  public OcrDtos.UploadReservationResponse reserveUpload(
      @Valid @RequestBody OcrDtos.UploadReservationRequest body) {
    return toResponse(
        service.reserveUpload(
            new OcrDocumentService.UploadCommand(
                body.filename(), body.mimeType(), body.checksum(), body.sizeBytes())));
  }

  @PostMapping("/ocr/jobs")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Start OCR extraction for an uploaded document")
  public OcrDtos.OcrJobResponse startJob(@Valid @RequestBody OcrDtos.StartOcrJobRequest body) {
    return toResponse(
        service.startJob(
            new OcrDocumentService.StartJobCommand(
                body.documentId(), body.documentType(), body.rawText())));
  }

  @GetMapping("/ocr/jobs")
  public List<OcrDtos.OcrJobResponse> listJobs() {
    return service.listJobs().stream().map(OcrDocumentController::toResponse).toList();
  }

  @GetMapping("/ocr/jobs/{id}")
  public OcrDtos.OcrJobResponse getJob(@PathVariable String id) {
    return toResponse(service.getJob(id));
  }

  @PostMapping("/ocr/jobs/{id}/review")
  public OcrDtos.OcrJobResponse review(
      @PathVariable String id, @RequestBody OcrDtos.ReviewOcrJobRequest body) {
    return toResponse(
        service.review(id, new OcrDocumentService.ReviewCommand(body.accepted(), body.fields())));
  }

  private static OcrDtos.UploadReservationResponse toResponse(
      OcrDocumentService.UploadReservation reservation) {
    return new OcrDtos.UploadReservationResponse(
        reservation.documentId(),
        reservation.storageKey(),
        reservation.uploadUrl(),
        reservation.expiresAt(),
        toResponse(reservation.document()));
  }

  private static OcrDtos.DocumentResponse toResponse(OcrDocumentService.DocumentResponse document) {
    return new OcrDtos.DocumentResponse(
        document.id(),
        document.storageKey(),
        document.originalFilename(),
        document.mimeType(),
        document.checksum(),
        document.sizeBytes(),
        document.createdAt());
  }

  private static OcrDtos.OcrJobResponse toResponse(OcrDocumentService.OcrJobResponse job) {
    return new OcrDtos.OcrJobResponse(
        job.id(),
        job.documentId(),
        job.documentType(),
        job.status(),
        job.fields(),
        job.confidence(),
        job.failureReason(),
        job.createdAt(),
        job.updatedAt(),
        job.reviewedAt(),
        job.reviewedBy());
  }
}
