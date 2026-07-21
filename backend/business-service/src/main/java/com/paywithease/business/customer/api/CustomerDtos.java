package com.paywithease.business.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public final class CustomerDtos {
  private CustomerDtos() {}

  public record CreateCustomer(
      @NotBlank String name,
      @NotBlank
          @Pattern(
              regexp = "(\\+?91)?[6-9]\\d{9}",
              message = "mobile must be a valid Indian mobile number")
          String mobile,
      String email,
      String address,
      String gstin) {}

  public record CustomerResponse(
      String id,
      String name,
      String mobile,
      String email,
      String address,
      String gstin,
      Instant createdAt) {}

  public record LedgerSummaryResponse(
      String customerId, long totalReceivableMinor, long totalReceivedMinor, int openInvoices) {}
}
