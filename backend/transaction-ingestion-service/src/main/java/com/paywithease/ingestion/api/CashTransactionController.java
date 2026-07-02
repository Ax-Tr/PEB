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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Manual cash transaction entry (always {@link TxnSource#MANUAL_CASH}). */
@RestController
@RequestMapping("/api/v1/cash-transactions")
@Tag(name = "cash-transactions", description = "Manual cash in/out entry")
public class CashTransactionController {

  private final IngestionService service;

  public CashTransactionController(IngestionService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a manual cash transaction")
  public IngestionDtos.TransactionResponse create(
      @Valid @RequestBody IngestionDtos.ManualTxnRequest body) {
    IngestionDtos.TxnRowDto row = body.row();
    if (!Direction.isValid(row.direction())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid direction: " + row.direction());
    }
    IngestionService.TxnRow txnRow =
        new IngestionService.TxnRow(
            Direction.valueOf(row.direction()),
            row.amountMinor(),
            row.txnDate(),
            row.narration(),
            row.externalRef(),
            row.counterparty());
    // This endpoint is cash-only: ignore any source in the body.
    Transaction txn = service.addManual(body.bankAccountId(), TxnSource.MANUAL_CASH, txnRow);
    return IngestionDtos.toResponse(txn);
  }
}
