package com.paywithease.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.domain.AccountType;
import com.paywithease.ledger.domain.Accounts;
import com.paywithease.ledger.domain.ChartOfAccount;
import com.paywithease.ledger.domain.FinancialPeriod;
import com.paywithease.ledger.domain.JournalCommand;
import com.paywithease.ledger.domain.JournalEntry;
import com.paywithease.ledger.domain.JournalEntryLine;
import com.paywithease.ledger.domain.LedgerBalance;
import com.paywithease.ledger.domain.PostingTemplates;
import com.paywithease.ledger.infrastructure.ChartOfAccountRepository;
import com.paywithease.ledger.infrastructure.FinancialPeriodRepository;
import com.paywithease.ledger.infrastructure.JournalEntryLineRepository;
import com.paywithease.ledger.infrastructure.JournalEntryRepository;
import com.paywithease.ledger.infrastructure.LedgerBalanceRepository;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class LedgerPostingServiceTest {

  @Mock JournalEntryRepository entries;
  @Mock JournalEntryLineRepository lines;
  @Mock LedgerBalanceRepository balances;
  @Mock ChartOfAccountRepository accounts;
  @Mock FinancialPeriodRepository periods;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private LedgerPostingService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new LedgerPostingService(
            entries, lines, balances, accounts, periods, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    // Accounts resolve for any code.
    when(accounts.findByTenantIdAndCode(eq("tenant1"), anyString()))
        .thenAnswer(
            inv ->
                Optional.of(
                    new ChartOfAccount(
                        "acc-" + inv.getArgument(1),
                        "tenant1",
                        inv.getArgument(1),
                        "n",
                        AccountType.ASSET,
                        false,
                        clock.instant())));
    when(entries.save(any())).thenAnswer(returnsFirstArg());
    when(lines.save(any())).thenAnswer(returnsFirstArg());
    when(balances.save(any())).thenAnswer(returnsFirstArg());
    when(balances.findByTenantIdAndAccountId(any(), any())).thenReturn(Optional.empty());
    when(periods.findByTenantIdAndYearAndMonth("tenant1", 2026, 5))
        .thenReturn(Optional.of(openPeriod()));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private FinancialPeriod openPeriod() {
    return new FinancialPeriod("per1", "tenant1", 2026, 5, clock.instant());
  }

  private JournalCommand invoiceCommand() {
    return PostingTemplates.customerInvoice(
        LocalDate.of(2026, 5, 15), 118000, 18000, "invoice-gst-service", "evt1", "corr1", "INV1");
  }

  @Test
  void postWritesEntryLinesAndUpdatesBalancesAndEmits() {
    when(entries.findByTenantIdAndSourceServiceAndSourceEventId(any(), any(), any()))
        .thenReturn(Optional.empty());

    String id = service.post(invoiceCommand(), "actor1");

    assertThat(id).isNotBlank();
    verify(entries).save(any(JournalEntry.class));
    verify(lines, times(3)).save(any(JournalEntryLine.class)); // Receivable, Sales, Output GST
    verify(balances, times(3)).save(any(LedgerBalance.class));
    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("JOURNAL_ENTRY_POSTED");
    verify(audit).record(eq("JOURNAL_ENTRY_POSTED"), any(), any(), any());
  }

  @Test
  void postIsIdempotentOnSourceEvent() {
    JournalEntry existing =
        new JournalEntry(
            "existing",
            "tenant1",
            LocalDate.of(2026, 5, 15),
            "n",
            "invoice-gst-service",
            "evt1",
            null,
            "per1",
            "corr1",
            "actor1",
            clock.instant());
    when(entries.findByTenantIdAndSourceServiceAndSourceEventId(
            "tenant1", "invoice-gst-service", "evt1"))
        .thenReturn(Optional.of(existing));

    String id = service.post(invoiceCommand(), "actor1");

    assertThat(id).isEqualTo("existing");
    verify(entries, never()).save(any());
    verify(outbox, never()).append(any());
  }

  @Test
  void postRejectedWhenPeriodLocked() {
    FinancialPeriod locked = openPeriod();
    locked.lock(clock.instant());
    when(periods.findByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(Optional.of(locked));
    when(entries.findByTenantIdAndSourceServiceAndSourceEventId(any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.post(invoiceCommand(), "actor1"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("locked");
    verify(entries, never()).save(any());
  }

  @Test
  void postFailsWhenAccountNotSeeded() {
    when(accounts.findByTenantIdAndCode(eq("tenant1"), anyString())).thenReturn(Optional.empty());
    when(entries.findByTenantIdAndSourceServiceAndSourceEventId(any(), any(), any()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.post(invoiceCommand(), "actor1"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("chart of accounts");
  }

  @Test
  void reverseCreatesOffsettingEntry() {
    JournalEntry original =
        new JournalEntry(
            "orig",
            "tenant1",
            LocalDate.of(2026, 5, 15),
            "INV1",
            "invoice-gst-service",
            "evt1",
            null,
            "per1",
            "corr1",
            "actor1",
            clock.instant());
    when(entries.findByTenantIdAndId("tenant1", "orig")).thenReturn(Optional.of(original));
    when(entries.existsByTenantIdAndReversalOf("tenant1", "orig")).thenReturn(false);
    when(entries.findByTenantIdAndSourceServiceAndSourceEventId(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(lines.findByJournalEntryId("orig"))
        .thenReturn(
            List.of(
                new JournalEntryLine(
                    "l1",
                    "tenant1",
                    "orig",
                    "acc-1100",
                    Accounts.ACCOUNTS_RECEIVABLE,
                    118000,
                    0,
                    "d"),
                new JournalEntryLine(
                    "l2", "tenant1", "orig", "acc-4000", Accounts.SALES_REVENUE, 0, 100000, "c"),
                new JournalEntryLine(
                    "l3", "tenant1", "orig", "acc-2100", Accounts.OUTPUT_GST, 0, 18000, "c")));
    when(periods.findByTenantIdAndYearAndMonth("tenant1", 2026, 5))
        .thenReturn(Optional.of(openPeriod()));

    String reversalId = service.reverse("orig", "customer returned goods", "actor1");

    assertThat(reversalId).isNotBlank();
    ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
    verify(entries).save(entryCaptor.capture());
    assertThat(entryCaptor.getValue().getReversalOf()).isEqualTo("orig");
    // Mirror: 3 lines re-posted with sides swapped.
    verify(lines, times(3)).save(any());
    verify(audit).record(eq("JOURNAL_REVERSED"), any(), eq("orig"), any());
  }

  @Test
  void reverseRejectsAlreadyReversed() {
    JournalEntry original =
        new JournalEntry(
            "orig",
            "tenant1",
            LocalDate.of(2026, 5, 15),
            "INV1",
            "invoice-gst-service",
            "evt1",
            null,
            "per1",
            "corr1",
            "actor1",
            clock.instant());
    when(entries.findByTenantIdAndId("tenant1", "orig")).thenReturn(Optional.of(original));
    when(entries.existsByTenantIdAndReversalOf("tenant1", "orig")).thenReturn(true);

    assertThatThrownBy(() -> service.reverse("orig", "x", "actor1"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already reversed");
  }
}
