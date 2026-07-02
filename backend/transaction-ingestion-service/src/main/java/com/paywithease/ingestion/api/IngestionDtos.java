package com.paywithease.ingestion.api;

import com.paywithease.ingestion.domain.BankAccount;
import com.paywithease.ingestion.domain.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs and mappers for the transaction-ingestion API. */
public final class IngestionDtos {

  private IngestionDtos() {}

  public record AddBankAccountRequest(
      @NotBlank String bankName,
      @NotBlank String accountNumber,
      String ifsc,
      String accountType,
      @PositiveOrZero long openingBalanceMinor) {}

  public record BankAccountResponse(
      String id,
      String bankName,
      String accountType,
      String maskedAccountNumber,
      long openingBalanceMinor) {}

  public record TxnRowDto(
      @NotBlank String direction,
      @Positive long amountMinor,
      @NotNull LocalDate txnDate,
      String narration,
      String externalRef,
      String counterparty) {}

  public record ManualTxnRequest(
      String bankAccountId, @NotBlank String source, @Valid @NotNull TxnRowDto row) {}

  public record ImportRequest(
      String bankAccountId,
      @NotBlank String source,
      String fileName,
      @NotEmpty List<@Valid TxnRowDto> rows) {}

  public record ImportResultResponse(String batchId, int totalRows, int imported, int duplicates) {}

  public record ReviewRequest(String category) {}

  public record TransactionResponse(
      String id,
      String source,
      String direction,
      long amountMinor,
      LocalDate txnDate,
      String narration,
      String externalRef,
      String category,
      String classificationStatus,
      BigDecimal classificationConfidence,
      boolean reconciled) {}

  // ---- Mappers ---------------------------------------------------------------

  public static BankAccountResponse toResponse(BankAccount a) {
    return new BankAccountResponse(
        a.getId(),
        a.getBankName(),
        a.getAccountType(),
        a.maskedAccountNumber(),
        a.getOpeningBalanceMinor());
  }

  public static TransactionResponse toResponse(Transaction t) {
    return new TransactionResponse(
        t.getId(),
        t.getSource(),
        t.getDirection(),
        t.getAmountMinor(),
        t.getTxnDate(),
        t.getNarration(),
        t.getExternalRef(),
        t.getCategory(),
        t.getClassificationStatus(),
        t.getClassificationConfidence(),
        t.isReconciled());
  }
}
