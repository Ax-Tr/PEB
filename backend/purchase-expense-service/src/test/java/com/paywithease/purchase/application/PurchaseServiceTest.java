package com.paywithease.purchase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.purchase.application.PurchaseService.CreateBillCommand;
import com.paywithease.purchase.application.PurchaseService.LineCommand;
import com.paywithease.purchase.domain.Expense;
import com.paywithease.purchase.domain.PurchaseBill;
import com.paywithease.purchase.infrastructure.ExpenseRepository;
import com.paywithease.purchase.infrastructure.PurchaseBillRepository;
import com.paywithease.purchase.infrastructure.PurchaseItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(
    strictness = Strictness.LENIENT) // shared @BeforeEach stubs; not every test uses all
class PurchaseServiceTest {

  @Mock private PurchaseBillRepository billRepo;
  @Mock private PurchaseItemRepository itemRepo;
  @Mock private ExpenseRepository expenseRepo;
  @Mock private AuditWriter audit;
  @Mock private OutboxWriter outbox;

  private PurchaseService service;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    Clock clock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);
    service =
        new PurchaseService(
            billRepo, itemRepo, expenseRepo, audit, outbox, objectMapper, clock, "27");

    when(billRepo.save(any())).thenAnswer(returnsFirstArg());
    when(itemRepo.save(any())).thenAnswer(returnsFirstArg());
    when(expenseRepo.save(any())).thenAnswer(returnsFirstArg());

    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private static LineCommand line(String rate) {
    return new LineCommand(
        "prod1", "Widget", "998314", BigDecimal.ONE, 100000L, 0L, new BigDecimal(rate));
  }

  private static CreateBillCommand bill(String placeOfSupply, String businessState) {
    return new CreateBillCommand(
        "vendor1",
        "Acme Supplies",
        "29ABCDE1234F1Z5",
        "BILL-001",
        placeOfSupply,
        businessState,
        false,
        LocalDate.of(2026, 5, 15),
        List.of(line("18")));
  }

  @Test
  void createBillComputesInputGstAndEmitsEvent() {
    PurchaseBill bill = service.createBill(bill("27", "27"));

    assertThat(bill.getTotalInputGstMinor()).isEqualTo(18000);
    assertThat(bill.getTotalCgstMinor()).isEqualTo(9000);
    assertThat(bill.getTotalSgstMinor()).isEqualTo(9000);
    assertThat(bill.getTotalAmountMinor()).isEqualTo(118000);

    verify(itemRepo, atLeastOnce()).save(any());
    verify(audit).record(eq("PURCHASE_BILL_CREATED"), eq("purchase_bill"), any(), any());

    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("PURCHASE_BILL_CREATED");
  }

  @Test
  void createBillInterStateInputIgst() {
    PurchaseBill bill = service.createBill(bill("29", "27"));

    assertThat(bill.getTotalIgstMinor()).isEqualTo(18000);
    assertThat(bill.getTotalCgstMinor()).isZero();
    assertThat(bill.getTotalSgstMinor()).isZero();
    assertThat(bill.getTotalAmountMinor()).isEqualTo(118000);
  }

  @Test
  void approveExpenseEmitsEvent() {
    Expense expense =
        service.createExpense(
            "TRAVEL", "Cab", 50000L, new BigDecimal("18"), "vendor1", LocalDate.of(2026, 5, 15));
    when(expenseRepo.findByTenantIdAndId("tenant1", expense.getId()))
        .thenReturn(Optional.of(expense));

    Expense approved = service.approveExpense(expense.getId(), "checker1");

    assertThat(approved.getStatus()).isEqualTo("APPROVED");
    assertThat(approved.getApprovedBy()).isEqualTo("checker1");
    verify(audit).record(eq("EXPENSE_APPROVED"), eq("expense"), eq(expense.getId()), any());

    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("EXPENSE_APPROVED");
  }

  @Test
  void approveExpenseRejectsNonPending() {
    Expense expense =
        service.createExpense(
            "TRAVEL", "Cab", 50000L, new BigDecimal("18"), "vendor1", LocalDate.of(2026, 5, 15));
    expense.approve("checker1", Instant.parse("2026-05-15T10:00:00Z"));
    when(expenseRepo.findByTenantIdAndId("tenant1", expense.getId()))
        .thenReturn(Optional.of(expense));

    assertThatThrownBy(() -> service.approveExpense(expense.getId(), "checker2"))
        .isInstanceOf(ApiException.class);
  }
}
