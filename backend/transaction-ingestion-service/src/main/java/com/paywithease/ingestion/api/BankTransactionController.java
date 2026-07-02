package com.paywithease.ingestion.api;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.ingestion.application.IngestionService;
import com.paywithease.ingestion.domain.Direction;
import com.paywithease.ingestion.domain.Transaction;
import com.paywithease.ingestion.domain.TxnSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bank transactions: manual bank entry, statement/UPI/settlement import, and review. */
@RestController
@RequestMapping("/api/v1/bank-transactions")
@Tag(name = "bank-transactions", description = "Manual bank entry, statement/feed import, review")
public class BankTransactionController {

  private final IngestionService service;

  public BankTransactionController(IngestionService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a manual bank transaction")
  public IngestionDtos.TransactionResponse create(
      @Valid @RequestBody IngestionDtos.ManualTxnRequest body) {
    IngestionDtos.TxnRowDto row = body.row();
    if (!Direction.isValid(row.direction())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid direction: " + row.direction());
    }
    Transaction txn = service.addManual(body.bankAccountId(), TxnSource.MANUAL_BANK, toRow(row));
    return IngestionDtos.toResponse(txn);
  }

  @PostMapping("/import")
  @Operation(summary = "Import statement/UPI/settlement rows (idempotent, deduplicated)")
  public IngestionDtos.ImportResultResponse importRows(
      @Valid @RequestBody IngestionDtos.ImportRequest body) {
    if (!TxnSource.isValid(body.source())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid source: " + body.source());
    }
    TxnSource source = TxnSource.valueOf(body.source());
    if (!source.isImport()) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "Source is not an import source: " + source);
    }
    List<IngestionService.TxnRow> rows = new ArrayList<>(body.rows().size());
    for (IngestionDtos.TxnRowDto row : body.rows()) {
      if (!Direction.isValid(row.direction())) {
        throw new ApiException(
            ErrorCode.VALIDATION_FAILED, "Invalid direction: " + row.direction());
      }
      rows.add(toRow(row));
    }
    IngestionService.ImportResult result =
        service.importRows(body.bankAccountId(), source, body.fileName(), rows);
    return new IngestionDtos.ImportResultResponse(
        result.batchId(), result.totalRows(), result.imported(), result.duplicates());
  }

  @GetMapping
  @Operation(summary = "List recent transactions")
  public List<IngestionDtos.TransactionResponse> recent() {
    return service.recent().stream().map(IngestionDtos::toResponse).toList();
  }

  @GetMapping("/review-queue")
  @Operation(summary = "List transactions awaiting classification review")
  public List<IngestionDtos.TransactionResponse> reviewQueue() {
    return service.reviewQueue().stream().map(IngestionDtos::toResponse).toList();
  }

  @PostMapping("/{id}/review")
  @Operation(summary = "Confirm/override a transaction's category")
  public IngestionDtos.TransactionResponse review(
      @PathVariable String id, @RequestBody IngestionDtos.ReviewRequest body) {
    return IngestionDtos.toResponse(service.reviewClassification(id, body.category()));
  }

  private static IngestionService.TxnRow toRow(IngestionDtos.TxnRowDto row) {
    return new IngestionService.TxnRow(
        Direction.valueOf(row.direction()),
        row.amountMinor(),
        row.txnDate(),
        row.narration(),
        row.externalRef(),
        row.counterparty());
  }
}
