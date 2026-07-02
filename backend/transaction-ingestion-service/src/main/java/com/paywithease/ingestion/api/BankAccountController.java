package com.paywithease.ingestion.api;

import com.paywithease.ingestion.application.IngestionService;
import com.paywithease.ingestion.domain.BankAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bank account registration for statement import & reconciliation. */
@RestController
@RequestMapping("/api/v1/bank-accounts")
@Tag(name = "bank-accounts", description = "Business bank accounts whose statements are imported")
public class BankAccountController {

  private final IngestionService service;

  public BankAccountController(IngestionService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a bank account")
  public IngestionDtos.BankAccountResponse create(
      @Valid @RequestBody IngestionDtos.AddBankAccountRequest body) {
    BankAccount account =
        service.addBankAccount(
            body.bankName(),
            body.accountNumber(),
            body.ifsc(),
            body.accountType(),
            body.openingBalanceMinor());
    return IngestionDtos.toResponse(account);
  }

  @GetMapping
  @Operation(summary = "List registered bank accounts")
  public List<IngestionDtos.BankAccountResponse> list() {
    return service.listBankAccounts().stream().map(IngestionDtos::toResponse).toList();
  }
}
