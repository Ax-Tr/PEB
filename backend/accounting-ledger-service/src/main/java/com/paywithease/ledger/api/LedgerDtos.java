package com.paywithease.ledger.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs for the ledger API. */
public final class LedgerDtos {

  private LedgerDtos() {}

  public record JournalLineDto(
      @NotBlank String accountCode,
      @PositiveOrZero long debitMinor,
      @PositiveOrZero long creditMinor,
      String narration) {}

  public record PostJournalRequest(
      @NotNull LocalDate entryDate, String narration, @NotEmpty List<JournalLineDto> lines) {}

  public record JournalLineResponse(
      String accountCode, long debitMinor, long creditMinor, String narration) {}

  public record JournalEntryResponse(
      String id,
      LocalDate entryDate,
      String narration,
      String sourceService,
      String sourceEventId,
      String status,
      String reversalOf,
      List<JournalLineResponse> lines) {}

  public record AccountResponse(
      String code, String name, String type, String normalSide, boolean contra) {}

  public record LedgerAccountResponse(
      String accountCode, long debitTotalMinor, long creditTotalMinor, long netBalanceMinor) {}

  public record ReverseRequest(String reason) {}

  public record PeriodActionRequest(String reason) {}

  public record PeriodResponse(int year, int month, String state) {}

  public record SeedResponse(int accountsCreated) {}
}
