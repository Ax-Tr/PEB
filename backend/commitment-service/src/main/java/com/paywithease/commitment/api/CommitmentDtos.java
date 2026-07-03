package com.paywithease.commitment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs for the commitment API. */
public final class CommitmentDtos {

  private CommitmentDtos() {}

  public record CreateCommitmentRequest(
      @NotBlank String counterpartyType,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      String description,
      @Positive long amountMinor,
      @NotNull LocalDate dueDate) {}

  public record RecordPaymentRequest(@Positive long amountMinor, String note) {}

  public record RescheduleRequest(@NotNull LocalDate newDueDate, String note) {}

  public record CancelRequest(String note) {}

  public record CommitmentResponse(
      String id,
      String counterpartyType,
      String counterpartyId,
      String counterpartyName,
      String sourceType,
      String sourceRef,
      String description,
      long amountMinor,
      long paidMinor,
      long outstandingMinor,
      LocalDate dueDate,
      String status,
      Instant createdAt,
      Instant updatedAt,
      Instant closedAt) {}

  public record CommitmentEventResponse(
      String id,
      String eventType,
      LocalDate oldDueDate,
      LocalDate newDueDate,
      Long amountMinor,
      String note,
      Instant occurredAt) {}

  public record BrokenMarkResponse(int changed) {}

  public record CommitmentDetailResponse(
      CommitmentResponse commitment, List<CommitmentEventResponse> events) {}
}
