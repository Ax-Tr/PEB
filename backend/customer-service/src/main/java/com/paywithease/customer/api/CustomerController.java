package com.paywithease.customer.api;

import com.paywithease.common.error.ApiException;
import com.paywithease.customer.application.CustomerService;
import com.paywithease.customer.application.CustomerService.LedgerSummary;
import com.paywithease.customer.domain.Customer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Customer directory API. */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "customers", description = "Customer directory, mobile lookup, ledger summary")
public class CustomerController {

  private final CustomerService service;

  public CustomerController(CustomerService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a customer (unique mobile per tenant)")
  public CustomerDtos.CustomerResponse create(
      @Valid @RequestBody CustomerDtos.CreateCustomer body) {
    Customer c =
        service.create(body.name(), body.mobile(), body.email(), body.address(), body.gstin());
    return toResponse(c);
  }

  @GetMapping
  @Operation(summary = "List customers (newest first)")
  public List<CustomerDtos.CustomerResponse> list() {
    return service.list().stream().map(this::toResponse).toList();
  }

  @GetMapping("/search")
  @Operation(summary = "Find a customer by mobile number")
  public CustomerDtos.CustomerResponse search(@RequestParam String mobile) {
    return service
        .searchByMobile(mobile)
        .map(this::toResponse)
        .orElseThrow(() -> ApiException.notFound("Customer"));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a customer by id")
  public CustomerDtos.CustomerResponse get(@PathVariable String id) {
    return toResponse(service.get(id));
  }

  @GetMapping("/{id}/ledger-summary")
  @Operation(summary = "Get a customer's ledger summary (Sprint-2 stub)")
  public CustomerDtos.LedgerSummaryResponse ledgerSummary(@PathVariable String id) {
    LedgerSummary s = service.ledgerSummary(id);
    return new CustomerDtos.LedgerSummaryResponse(
        s.customerId(), s.totalReceivableMinor(), s.totalReceivedMinor(), s.openInvoices());
  }

  private CustomerDtos.CustomerResponse toResponse(Customer c) {
    return new CustomerDtos.CustomerResponse(
        c.getId(),
        c.getName(),
        c.getMobile(),
        c.getEmail(),
        c.getAddress(),
        c.getGstin(),
        c.getCreatedAt());
  }
}
