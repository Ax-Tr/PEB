package com.paywithease.ingestion.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ingestion.domain.BankAccount;
import com.paywithease.ingestion.domain.DedupeKey;
import com.paywithease.ingestion.domain.Direction;
import com.paywithease.ingestion.domain.ImportBatch;
import com.paywithease.ingestion.domain.Transaction;
import com.paywithease.ingestion.domain.TransactionClassifier;
import com.paywithease.ingestion.domain.TxnSource;
import com.paywithease.ingestion.infrastructure.BankAccountRepository;
import com.paywithease.ingestion.infrastructure.ImportBatchRepository;
import com.paywithease.ingestion.infrastructure.TransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Captures cash/bank/UPI/settlement transactions: manual entry, idempotent statement/feed import
 * with duplicate detection, rule-based classification with confidence, and human review. Imported
 * rows are the external source of truth for reconciliation — they are not posted to the ledger
 * here.
 */
@Service
public class IngestionService {

  private static final String SOURCE = "transaction-ingestion-service";

  private final BankAccountRepository accounts;
  private final TransactionRepository transactions;
  private final ImportBatchRepository batches;
  private final BlindIndex blindIndex;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final BigDecimal autoConfirmThreshold;

  public IngestionService(
      BankAccountRepository accounts,
      TransactionRepository transactions,
      ImportBatchRepository batches,
      BlindIndex blindIndex,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.ingestion.auto-confirm-confidence:0.90}") String autoConfirmThreshold) {
    this.accounts = accounts;
    this.transactions = transactions;
    this.batches = batches;
    this.blindIndex = blindIndex;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.autoConfirmThreshold = new BigDecimal(autoConfirmThreshold);
  }

  // ---- Bank accounts ---------------------------------------------------------

  @Transactional
  public BankAccount addBankAccount(
      String bankName,
      String accountNumber,
      String ifsc,
      String accountType,
      long openingBalanceMinor) {
    String tenantId = TenantContext.requireTenantId();
    String hash = blindIndex.hash(accountNumber);
    if (accounts.existsByTenantIdAndAccountNumberHash(tenantId, hash)) {
      throw new ApiException(ErrorCode.CONFLICT, "Bank account already exists");
    }
    BankAccount account =
        new BankAccount(
            Ulid.newId(),
            tenantId,
            bankName,
            accountNumber,
            hash,
            ifsc,
            accountType,
            openingBalanceMinor,
            clock.instant());
    accounts.save(account);
    audit.record("BANK_ACCOUNT_ADDED", "bank_account", account.getId(), Map.of("bank", bankName));
    return account;
  }

  @Transactional(readOnly = true)
  public List<BankAccount> listBankAccounts() {
    return accounts.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  // ---- Transactions ----------------------------------------------------------

  public record TxnRow(
      Direction direction,
      long amountMinor,
      LocalDate txnDate,
      String narration,
      String externalRef,
      String counterparty) {}

  @Transactional
  public Transaction addManual(String bankAccountId, TxnSource source, TxnRow row) {
    String tenantId = TenantContext.requireTenantId();
    if (!source.name().startsWith("MANUAL")) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "Manual entry requires a MANUAL_* source");
    }
    // Manual entries are never deduped against each other (a unique ref makes each distinct).
    String dedupe =
        DedupeKey.compute(
            tenantId,
            bankAccountId,
            source,
            row.direction(),
            row.amountMinor(),
            row.txnDate(),
            Ulid.newId(),
            row.narration());
    Transaction txn = persist(tenantId, bankAccountId, source, row, dedupe, null);
    audit.record(
        "MANUAL_TRANSACTION_ADDED", "transaction", txn.getId(), Map.of("source", source.name()));
    if ("CONFIRMED".equals(txn.getClassificationStatus())) {
      emit("TRANSACTION_CLASSIFIED", txn);
    }
    return txn;
  }

  public record ImportResult(String batchId, int totalRows, int imported, int duplicates) {}

  @Transactional
  public ImportResult importRows(
      String bankAccountId, TxnSource source, String fileName, List<TxnRow> rows) {
    String tenantId = TenantContext.requireTenantId();
    if (!source.isImport()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Import requires an import source");
    }
    Instant now = clock.instant();
    ImportBatch batch =
        new ImportBatch(Ulid.newId(), tenantId, bankAccountId, source, fileName, rows.size(), now);
    batches.save(batch);

    for (TxnRow row : rows) {
      String dedupe =
          DedupeKey.compute(
              tenantId,
              bankAccountId,
              source,
              row.direction(),
              row.amountMinor(),
              row.txnDate(),
              row.externalRef(),
              row.narration());
      if (transactions.existsByTenantIdAndDedupeHash(tenantId, dedupe)) {
        batch.recordDuplicate();
        continue;
      }
      Transaction txn = persist(tenantId, bankAccountId, source, row, dedupe, batch.getId());
      batch.recordImported();
      emit("BANK_TRANSACTION_IMPORTED", txn);
      if ("CONFIRMED".equals(txn.getClassificationStatus())) {
        emit("TRANSACTION_CLASSIFIED", txn);
      }
    }
    batches.save(batch);
    audit.record(
        "TRANSACTIONS_IMPORTED",
        "import_batch",
        batch.getId(),
        Map.of("imported", batch.getImportedCount(), "duplicates", batch.getDuplicateCount()));
    return new ImportResult(
        batch.getId(), batch.getTotalRows(), batch.getImportedCount(), batch.getDuplicateCount());
  }

  @Transactional
  public Transaction reviewClassification(String txnId, String category) {
    Transaction txn =
        transactions
            .findByTenantIdAndId(TenantContext.requireTenantId(), txnId)
            .orElseThrow(() -> ApiException.notFound("Transaction"));
    txn.confirm(category, clock.instant());
    transactions.save(txn);
    audit.record(
        "TRANSACTION_REVIEWED",
        "transaction",
        txnId,
        Map.of("category", category == null ? "" : category));
    emit("TRANSACTION_CLASSIFIED", txn);
    return txn;
  }

  @Transactional(readOnly = true)
  public List<Transaction> reviewQueue() {
    return transactions.findByTenantIdAndClassificationStatusOrderByTxnDateDesc(
        TenantContext.requireTenantId(), "SUGGESTED");
  }

  @Transactional(readOnly = true)
  public List<Transaction> recent() {
    return transactions.findTop200ByTenantIdOrderByTxnDateDesc(TenantContext.requireTenantId());
  }

  @Transactional(readOnly = true)
  public Transaction get(String id) {
    return transactions
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Transaction"));
  }

  private Transaction persist(
      String tenantId,
      String bankAccountId,
      TxnSource source,
      TxnRow row,
      String dedupe,
      String batchId) {
    Instant now = clock.instant();
    Transaction txn =
        new Transaction(
            Ulid.newId(),
            tenantId,
            bankAccountId,
            source,
            row.direction(),
            row.amountMinor(),
            row.txnDate(),
            row.narration(),
            row.externalRef(),
            row.counterparty(),
            dedupe,
            batchId,
            now);
    TransactionClassifier.Suggestion s =
        TransactionClassifier.classify(row.narration(), row.direction());
    boolean autoConfirm = s.confidence().compareTo(autoConfirmThreshold) >= 0;
    txn.applySuggestion(s.category(), s.confidence(), autoConfirm, now);
    return transactions.save(txn);
  }

  private void emit(String eventType, Transaction txn) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("transactionId", txn.getId());
    payload.put("source", txn.getSource());
    payload.put("direction", txn.getDirection());
    payload.put("amountMinor", txn.getAmountMinor());
    payload.put("category", txn.getCategory());
    payload.put("classificationStatus", txn.getClassificationStatus());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(txn.getTenantId())
            .businessId(txn.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(txn.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
