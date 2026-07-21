package com.paywithease.business.vendor.api;

import com.paywithease.business.vendor.application.VendorService;
import com.paywithease.business.vendor.domain.Vendor;
import com.paywithease.business.vendor.domain.VendorBankAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendors")
@Tag(name = "vendors", description = "Vendor profiles and OCR-reviewed payout bank accounts")
public class VendorController {

  private final VendorService service;

  public VendorController(VendorService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a vendor")
  public VendorDtos.VendorResponse create(@Valid @RequestBody VendorDtos.CreateVendor body) {
    Vendor v =
        service.create(body.name(), body.mobile(), body.email(), body.gstin(), body.address());
    return toResponse(v);
  }

  @GetMapping
  @Operation(summary = "List vendors for the current tenant")
  public VendorDtos.VendorList list() {
    return new VendorDtos.VendorList(service.list().stream().map(this::toResponse).toList());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a vendor")
  public VendorDtos.VendorResponse get(@PathVariable String id) {
    return toResponse(service.get(id));
  }

  @PostMapping("/{id}/bank-accounts")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Add a bank account (saved PENDING_REVIEW)")
  public VendorDtos.BankAccountResponse addBankAccount(
      @PathVariable String id, @Valid @RequestBody VendorDtos.AddBankAccount body) {
    VendorBankAccount account =
        service.addBankAccount(
            id,
            body.accountNumber(),
            body.ifsc(),
            body.upi(),
            body.bankName(),
            body.holderName(),
            body.source());
    return toBankAccount(account);
  }

  @PostMapping("/{id}/bank-accounts/{baId}/confirm")
  @Operation(summary = "Confirm a pending bank account (marks it VERIFIED)")
  public VendorDtos.BankAccountResponse confirmBankAccount(
      @PathVariable String id, @PathVariable String baId, @AuthenticationPrincipal Jwt jwt) {
    return toBankAccount(service.confirmBankAccount(id, baId, jwt.getSubject()));
  }

  @PostMapping("/{id}/bank-accounts/{baId}/reject")
  @Operation(summary = "Reject a pending bank account")
  public VendorDtos.BankAccountResponse rejectBankAccount(
      @PathVariable String id, @PathVariable String baId, @AuthenticationPrincipal Jwt jwt) {
    return toBankAccount(service.rejectBankAccount(id, baId, jwt.getSubject()));
  }

  @GetMapping("/{id}/bank-accounts")
  @Operation(summary = "List a vendor's bank accounts")
  public VendorDtos.BankAccountList listBankAccounts(@PathVariable String id) {
    return new VendorDtos.BankAccountList(
        service.listBankAccounts(id).stream().map(this::toBankAccount).toList());
  }

  private VendorDtos.VendorResponse toResponse(Vendor v) {
    return new VendorDtos.VendorResponse(
        v.getId(),
        v.getName(),
        v.getMobile(),
        v.getEmail(),
        v.getGstin(),
        v.getAddress(),
        v.getStatus());
  }

  private VendorDtos.BankAccountResponse toBankAccount(VendorBankAccount a) {
    return new VendorDtos.BankAccountResponse(
        a.getId(),
        a.getVendorId(),
        maskAccountNumber(a.getAccountNumber()),
        a.getIfsc(),
        a.getUpi(),
        a.getBankName(),
        a.getHolderName(),
        a.getStatus().name(),
        a.getSource().name(),
        a.getReviewedBy());
  }

  private static String maskAccountNumber(String accountNumber) {
    if (accountNumber == null) return null;
    int len = accountNumber.length();
    if (len <= 4) return "*".repeat(len);
    return "*".repeat(len - 4) + accountNumber.substring(len - 4);
  }
}
