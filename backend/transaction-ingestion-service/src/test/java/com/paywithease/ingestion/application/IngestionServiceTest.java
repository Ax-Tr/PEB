package com.paywithease.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ingestion.domain.Direction;
import com.paywithease.ingestion.domain.Transaction;
import com.paywithease.ingestion.domain.TxnSource;
import com.paywithease.ingestion.infrastructure.BankAccountRepository;
import com.paywithease.ingestion.infrastructure.ImportBatchRepository;
import com.paywithease.ingestion.infrastructure.TransactionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IngestionServiceTest {

  @Mock BankAccountRepository accounts;
  @Mock TransactionRepository transactions;
  @Mock ImportBatchRepository batches;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private IngestionService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate D = LocalDate.of(2026, 7, 1);

  @BeforeEach
  void setUp() {
    service =
        new IngestionService(
            accounts,
            transactions,
            batches,
            new BlindIndex(new byte[32]),
            audit,
            outbox,
            objectMapper,
            clock,
            "0.90");
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(transactions.save(any())).thenAnswer(returnsFirstArg());
    when(batches.save(any())).thenAnswer(returnsFirstArg());
    when(accounts.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private IngestionService.TxnRow row(long amt, String narration, String ref, Direction dir) {
    return new IngestionService.TxnRow(dir, amt, D, narration, ref, "cp");
  }

  @Test
  void addBankAccountRejectsDuplicate() {
    when(accounts.existsByTenantIdAndAccountNumberHash(eq("tenant1"), any())).thenReturn(true);
    assertThatThrownBy(
            () -> service.addBankAccount("HDFC", "123456789012", "HDFC0001", "CURRENT", 0))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void importDedupesAndClassifies() {
    // row1 new (SALARY -> high confidence, auto-confirmed), row2 is a duplicate
    when(transactions.existsByTenantIdAndDedupeHash(eq("tenant1"), any()))
        .thenReturn(false)
        .thenReturn(true);

    IngestionService.ImportResult result =
        service.importRows(
            "acc1",
            TxnSource.BANK_IMPORT,
            "june.csv",
            List.of(
                row(5000000, "SALARY JUNE", "UTR1", Direction.DEBIT),
                row(4000, "duplicate row", "UTR2", Direction.DEBIT)));

    assertThat(result.imported()).isEqualTo(1);
    assertThat(result.duplicates()).isEqualTo(1);
    verify(transactions, times(1)).save(any(Transaction.class));

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactions).save(captor.capture());
    assertThat(captor.getValue().getCategory()).isEqualTo("SALARY");
    assertThat(captor.getValue().getClassificationStatus())
        .isEqualTo("CONFIRMED"); // 0.90 >= threshold
  }

  @Test
  void lowConfidenceImportStaysSuggestedForReview() {
    when(transactions.existsByTenantIdAndDedupeHash(eq("tenant1"), any())).thenReturn(false);

    service.importRows(
        "acc1",
        TxnSource.BANK_IMPORT,
        "f.csv",
        List.of(row(1234, "random purchase", "R1", Direction.DEBIT)));

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactions).save(captor.capture());
    assertThat(captor.getValue().getClassificationStatus())
        .isEqualTo("SUGGESTED"); // 0.30 < threshold
    assertThat(captor.getValue().getCategory()).isEqualTo("UNKNOWN");
  }

  @Test
  void reviewConfirmsAndEmits() {
    Transaction txn =
        new Transaction(
            "txn1",
            "tenant1",
            "acc1",
            TxnSource.BANK_IMPORT,
            Direction.DEBIT,
            1234,
            D,
            "random",
            "R1",
            "cp",
            "hash",
            "batch1",
            clock.instant());
    when(transactions.findByTenantIdAndId("tenant1", "txn1"))
        .thenReturn(java.util.Optional.of(txn));

    Transaction result = service.reviewClassification("txn1", "OFFICE_EXPENSE");

    assertThat(result.getClassificationStatus()).isEqualTo("CONFIRMED");
    assertThat(result.getCategory()).isEqualTo("OFFICE_EXPENSE");
    verify(outbox).append(any()); // TRANSACTION_CLASSIFIED
  }

  @Test
  void importRejectsNonImportSource() {
    assertThatThrownBy(
            () ->
                service.importRows(
                    "acc1",
                    TxnSource.MANUAL_CASH,
                    "f",
                    List.of(row(100, "x", null, Direction.DEBIT))))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("import source");
  }
}
