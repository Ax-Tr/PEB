package com.paywithease.installment.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs for the installment API. */
public final class InstallmentDtos {

  private InstallmentDtos() {}

  public record CreateScheduleRequest(
      @NotBlank String type,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      @Positive long totalAmountMinor,
      @Min(1) int numberOfEmis,
      @NotNull LocalDate firstDueDate,
      String frequency) {}

  public record PayEmiRequest(@Min(1) int emiNumber, @Positive long amountMinor) {}

  public record ModifyRequest(
      @Min(1) int numberOfEmis, @NotNull LocalDate firstDueDate, String frequency) {}

  public record EmiResponse(
      String id,
      int emiNumber,
      LocalDate dueDate,
      long amountMinor,
      long paidMinor,
      String status) {}

  public record InstallmentResponse(
      String id,
      String type,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      long totalAmountMinor,
      long outstandingMinor,
      int numberOfEmis,
      String frequency,
      String status,
      List<EmiResponse> emis) {}
}
