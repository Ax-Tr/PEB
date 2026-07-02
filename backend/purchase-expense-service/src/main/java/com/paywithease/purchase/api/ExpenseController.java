package com.paywithease.purchase.api;

import com.paywithease.purchase.application.PurchaseService;
import com.paywithease.purchase.domain.Expense;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expense recording and maker-checker approval API. */
@RestController
@RequestMapping("/api/v1/expenses")
@Tag(name = "expenses", description = "Business expenses with maker-checker approval")
public class ExpenseController {

  private final PurchaseService service;

  public ExpenseController(PurchaseService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a business expense (pending approval)")
  public PurchaseDtos.ExpenseResponse create(
      @Valid @RequestBody PurchaseDtos.CreateExpenseRequest body) {
    Expense expense =
        service.createExpense(
            body.category(),
            body.description(),
            body.amountMinor(),
            body.gstRate(),
            body.vendorId(),
            body.expenseDate());
    return toResponse(expense);
  }

  @GetMapping
  @Operation(summary = "List expenses")
  public List<PurchaseDtos.ExpenseResponse> list() {
    return service.listExpenses().stream().map(ExpenseController::toResponse).toList();
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Approve a pending expense (checker)")
  public PurchaseDtos.ExpenseResponse approve(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    String approverId = jwt.getSubject();
    return toResponse(service.approveExpense(id, approverId));
  }

  private static PurchaseDtos.ExpenseResponse toResponse(Expense e) {
    return new PurchaseDtos.ExpenseResponse(
        e.getId(),
        e.getCategory(),
        e.getDescription(),
        e.getAmountMinor(),
        e.getGstRate(),
        e.getInputGstMinor(),
        e.getVendorId(),
        e.getExpenseDate(),
        e.getStatus(),
        e.getApprovedBy());
  }
}
