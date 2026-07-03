package com.paywithease.ocr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ocr.domain.BankDetailExtractor;
import com.paywithease.ocr.domain.DocumentRecord;
import com.paywithease.ocr.domain.OcrJob;
import com.paywithease.ocr.infrastructure.DocumentRecordRepository;
import com.paywithease.ocr.infrastructure.OcrJobRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OcrDocumentServiceTest {

  @Mock DocumentRecordRepository documents;
  @Mock OcrJobRepository jobs;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private OcrDocumentService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new OcrDocumentService(
            documents,
            jobs,
            new DevelopmentOcrProvider(new BankDetailExtractor()),
            audit,
            outbox,
            new ObjectMapper(),
            clock,
            10_000_000,
            15);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(documents.save(any())).thenAnswer(returnsFirstArg());
    when(jobs.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void reserveUploadRejectsInvalidMimeType() {
    assertThatThrownBy(
            () ->
                service.reserveUpload(
                    new OcrDocumentService.UploadCommand("bank.txt", "text/plain", null, 100)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Unsupported OCR document type");
  }

  @Test
  void startJobExtractsBankDetailsAndRequiresReview() {
    DocumentRecord document =
        new DocumentRecord(
            "doc1",
            "tenant1",
            "tenant1/doc1/cheque.pdf",
            "cheque.pdf",
            "application/pdf",
            null,
            1000,
            "actor1",
            clock.instant());
    when(documents.findByTenantIdAndId("tenant1", "doc1")).thenReturn(Optional.of(document));

    OcrDocumentService.OcrJobResponse response =
        service.startJob(
            new OcrDocumentService.StartJobCommand(
                "doc1",
                "BANK_DETAILS",
                "ICICI Bank Account 123456789012 IFSC ICIC0000123 Holder: MEERA IYER"));

    assertThat(response.status()).isEqualTo("REVIEW_REQUIRED");
    assertThat(response.fields()).containsKeys("accountNumber", "ifsc", "holderName", "bankName");
    assertThat(response.confidence()).isGreaterThan(BigDecimal.valueOf(0.70));
    verify(audit).record(eq("OCR_REVIEW_REQUIRED"), eq("ocr_job"), any(), any());
    verify(outbox).append(any(EventEnvelope.class));
  }

  @Test
  void reviewAcceptedCompletesJob() {
    OcrJob job =
        new OcrJob(
            "job1",
            "tenant1",
            "doc1",
            com.paywithease.ocr.domain.DocumentType.BANK_DETAILS,
            clock.instant());
    job.applyExtraction(
        "HDFC Bank Account 123456789 IFSC HDFC0001234 Holder: RAVI KUMAR",
        """
        {"accountNumber":{"value":"123456789"},"ifsc":{"value":"HDFC0001234"},"bankName":{"value":"HDFC Bank"},"holderName":{"value":"RAVI KUMAR"}}
        """,
        BigDecimal.valueOf(0.91),
        clock.instant());
    when(jobs.findByTenantIdAndId("tenant1", "job1")).thenReturn(Optional.of(job));

    OcrDocumentService.OcrJobResponse response =
        service.review(
            "job1",
            new OcrDocumentService.ReviewCommand(
                true,
                Map.of(
                    "accountNumber",
                    Map.of("value", "123456789"),
                    "ifsc",
                    Map.of("value", "HDFC0001234"),
                    "bankName",
                    Map.of("value", "HDFC Bank"),
                    "holderName",
                    Map.of("value", "RAVI KUMAR"))));

    assertThat(response.status()).isEqualTo("COMPLETED");
    assertThat(response.reviewedBy()).isEqualTo("actor1");
    verify(audit).record(eq("OCR_ACCEPTED"), eq("ocr_job"), eq("job1"), any());
  }

  @Test
  void reviewAcceptedCanUseExistingExtractedFields() {
    OcrJob job =
        new OcrJob(
            "job1",
            "tenant1",
            "doc1",
            com.paywithease.ocr.domain.DocumentType.CHEQUE,
            clock.instant());
    job.applyExtraction(
        "ICICI Bank Account 123456789012 IFSC ICIC0000123 Holder: MEERA IYER",
        """
        {"accountNumber":{"value":"123456789012"},"ifsc":{"value":"ICIC0000123"},"bankName":{"value":"ICICI Bank"},"holderName":{"value":"MEERA IYER"}}
        """,
        BigDecimal.valueOf(0.93),
        clock.instant());
    when(jobs.findByTenantIdAndId("tenant1", "job1")).thenReturn(Optional.of(job));

    OcrDocumentService.OcrJobResponse response =
        service.review("job1", new OcrDocumentService.ReviewCommand(true, null));

    assertThat(response.status()).isEqualTo("COMPLETED");
    assertThat(response.fields()).containsKeys("accountNumber", "ifsc", "bankName", "holderName");
  }

  @Test
  void bankReviewRejectsMissingRequiredField() {
    OcrJob job =
        new OcrJob(
            "job1",
            "tenant1",
            "doc1",
            com.paywithease.ocr.domain.DocumentType.BANK_DETAILS,
            clock.instant());
    job.applyExtraction(
        "HDFC Bank Account 123456789 IFSC HDFC0001234",
        "{}",
        BigDecimal.valueOf(0.50),
        clock.instant());
    when(jobs.findByTenantIdAndId("tenant1", "job1")).thenReturn(Optional.of(job));

    assertThatThrownBy(
            () ->
                service.review(
                    "job1",
                    new OcrDocumentService.ReviewCommand(
                        true,
                        Map.of(
                            "accountNumber",
                            Map.of("value", "123456789"),
                            "ifsc",
                            Map.of("value", "HDFC0001234"),
                            "holderName",
                            Map.of("value", "RAVI KUMAR")))))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("bankName");
    verify(jobs, never()).save(job);
  }

  @Test
  void bankReviewRejectsInvalidIfsc() {
    OcrJob job =
        new OcrJob(
            "job1",
            "tenant1",
            "doc1",
            com.paywithease.ocr.domain.DocumentType.PASSBOOK,
            clock.instant());
    job.applyExtraction(
        "Account 123456789 Holder: RAVI KUMAR", "{}", BigDecimal.valueOf(0.50), clock.instant());
    when(jobs.findByTenantIdAndId("tenant1", "job1")).thenReturn(Optional.of(job));

    assertThatThrownBy(
            () ->
                service.review(
                    "job1",
                    new OcrDocumentService.ReviewCommand(
                        true,
                        Map.of(
                            "accountNumber",
                            "123456789",
                            "ifsc",
                            "BAD123",
                            "bankName",
                            "HDFC Bank",
                            "holderName",
                            "RAVI KUMAR"))))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("IFSC");
    verify(jobs, never()).save(job);
  }

  @Test
  void reviewBeforeExtractionIsRejected() {
    OcrJob job =
        new OcrJob(
            "job1",
            "tenant1",
            "doc1",
            com.paywithease.ocr.domain.DocumentType.INVOICE,
            clock.instant());
    when(jobs.findByTenantIdAndId("tenant1", "job1")).thenReturn(Optional.of(job));

    assertThatThrownBy(
            () -> service.review("job1", new OcrDocumentService.ReviewCommand(true, Map.of())))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("pending review");
    verify(jobs, never()).save(job);
  }

  @Test
  void completedAcceptanceIsIdempotent() {
    OcrJob job =
        new OcrJob(
            "job1",
            "tenant1",
            "doc1",
            com.paywithease.ocr.domain.DocumentType.BANK_DETAILS,
            clock.instant());
    job.applyExtraction(
        "HDFC Bank Account 123456789 IFSC HDFC0001234 Holder: RAVI KUMAR",
        """
        {"accountNumber":{"value":"123456789"},"ifsc":{"value":"HDFC0001234"},"bankName":{"value":"HDFC Bank"},"holderName":{"value":"RAVI KUMAR"}}
        """,
        BigDecimal.valueOf(0.91),
        clock.instant());
    job.review(true, null, "actor1", clock.instant());
    when(jobs.findByTenantIdAndId("tenant1", "job1")).thenReturn(Optional.of(job));

    OcrDocumentService.OcrJobResponse response =
        service.review("job1", new OcrDocumentService.ReviewCommand(true, Map.of()));

    assertThat(response.status()).isEqualTo("COMPLETED");
    verify(jobs, never()).save(job);
  }

  @Test
  void completedJobCannotBeRejectedLater() {
    OcrJob job =
        new OcrJob(
            "job1",
            "tenant1",
            "doc1",
            com.paywithease.ocr.domain.DocumentType.BANK_DETAILS,
            clock.instant());
    job.applyExtraction(
        "HDFC Bank Account 123456789 IFSC HDFC0001234 Holder: RAVI KUMAR",
        """
        {"accountNumber":{"value":"123456789"},"ifsc":{"value":"HDFC0001234"},"bankName":{"value":"HDFC Bank"},"holderName":{"value":"RAVI KUMAR"}}
        """,
        BigDecimal.valueOf(0.91),
        clock.instant());
    job.review(true, null, "actor1", clock.instant());
    when(jobs.findByTenantIdAndId("tenant1", "job1")).thenReturn(Optional.of(job));

    assertThatThrownBy(
            () -> service.review("job1", new OcrDocumentService.ReviewCommand(false, Map.of())))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("pending review");
    verify(jobs, never()).save(job);
  }
}
