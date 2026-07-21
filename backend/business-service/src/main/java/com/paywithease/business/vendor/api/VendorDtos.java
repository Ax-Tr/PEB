package com.paywithease.business.vendor.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public final class VendorDtos {
  private VendorDtos() {}

  public record CreateVendor(
      @NotBlank String name,
      @Pattern(regexp = "(\\+?91)?[6-9]\\d{9}", message = "invalid Indian mobile number")
          String mobile,
      String email,
      String gstin,
      String address) {}

  public record VendorResponse(
      String id,
      String name,
      String mobile,
      String email,
      String gstin,
      String address,
      String status) {}

  public record AddBankAccount(
      @NotBlank String accountNumber,
      @NotBlank String ifsc,
      String upi,
      @NotBlank String bankName,
      @NotBlank String holderName,
      @NotBlank @Pattern(regexp = "MANUAL|OCR", message = "source must be MANUAL or OCR")
          String source) {}

  public record BankAccountResponse(
      String id,
      String vendorId,
      String accountNumberMasked,
      String ifsc,
      String upi,
      String bankName,
      String holderName,
      String status,
      String source,
      String reviewedBy) {}

  public record VendorList(List<VendorResponse> vendors) {}

  public record BankAccountList(List<BankAccountResponse> bankAccounts) {}
}
