package com.paywithease.invoice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.invoice.application.InvoiceService.CreateCommand;
import com.paywithease.invoice.application.InvoiceService.LineCommand;
import com.paywithease.invoice.domain.DocumentType;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.infrastructure.GstTaxLineRepository;
import com.paywithease.invoice.infrastructure.InvoiceItemRepository;
import com.paywithease.invoice.infrastructure.InvoiceRepository;
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
class InvoiceServiceTest {

  @Mock private InvoiceRepository invoiceRepo;
  @Mock private InvoiceItemRepository itemRepo;
  @Mock private GstTaxLineRepository taxLineRepo;
  @Mock private InvoiceNumberService numberService;
  @Mock private AuditWriter audit;
  @Mock private OutboxWriter outbox;

  private InvoiceService service;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    Clock clock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);
    service =
        new InvoiceService(
            invoiceRepo,
            itemRepo,
            taxLineRepo,
            numberService,
            audit,
            outbox,
            objectMapper,
            clock,
            "INV",
            "BOS",
            "RCV",
            "CRN",
            "DBN",
            "27");

    when(invoiceRepo.save(any())).thenAnswer(returnsFirstArg());
    when(itemRepo.save(any())).thenAnswer(returnsFirstArg());
    when(taxLineRepo.save(any())).thenAnswer(returnsFirstArg());
    when(numberService.next(any(), any(), any(), any())).thenReturn("INV/2026-27/00001");
    when(numberService.financialYear(any())).thenReturn("2026-27");

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

  private static CreateCommand invoice(String placeOfSupply, String businessState) {
    return new CreateCommand(
        DocumentType.TAX_INVOICE.name(),
        "B2B",
        "cust1",
        "Acme",
        "29ABCDE1234F1Z5",
        placeOfSupply,
        businessState,
        false,
        LocalDate.of(2026, 5, 15),
        List.of(line("18")),
        null,
        null);
  }

  @Test
  void createIntraStateInvoiceComputesGstAndEmitsEvent() {
    Invoice inv = service.create(invoice("27", "27"));

    assertThat(inv.getTotalTaxMinor()).isEqualTo(18000);
    assertThat(inv.getTotalCgstMinor()).isEqualTo(9000);
    assertThat(inv.getTotalSgstMinor()).isEqualTo(9000);
    assertThat(inv.getTotalAmountMinor()).isEqualTo(118000);
    assertThat(inv.getInvoiceNumber()).isEqualTo("INV/2026-27/00001");

    verify(itemRepo, atLeastOnce()).save(any());
    verify(taxLineRepo, atLeastOnce()).save(any());
    verify(audit).record(eq("INVOICE_GENERATED"), eq("invoice"), any(), any());

    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("INVOICE_GENERATED");
  }

  @Test
  void createInterStateChargesIgst() {
    Invoice inv = service.create(invoice("29", "27"));

    assertThat(inv.getTotalIgstMinor()).isEqualTo(18000);
    assertThat(inv.getTotalCgstMinor()).isZero();
    assertThat(inv.getTotalSgstMinor()).isZero();
    assertThat(inv.getTotalAmountMinor()).isEqualTo(118000);
  }

  @Test
  void createCreditNoteSetsTypeAndOriginal() {
    CreateCommand cmd =
        new CreateCommand(
            DocumentType.CREDIT_NOTE.name(),
            "B2B",
            "cust1",
            "Acme",
            "29ABCDE1234F1Z5",
            "27",
            "27",
            false,
            LocalDate.of(2026, 5, 15),
            List.of(line("18")),
            "inv-x",
            "return");

    Invoice inv = service.createCreditNote(cmd);

    assertThat(inv.getDocumentType()).isEqualTo("CREDIT_NOTE");
    assertThat(inv.getOriginalDocumentId()).isEqualTo("inv-x");
  }

  @Test
  void markSentEmitsInvoiceSent() {
    Invoice issued =
        new Invoice(
            "inv1",
            "tenant1",
            DocumentType.TAX_INVOICE.name(),
            "B2B",
            "cust1",
            "Acme",
            "29ABCDE1234F1Z5",
            "27",
            "27",
            "INV/2026-27/00001",
            "2026-27",
            LocalDate.of(2026, 5, 15),
            null,
            null,
            false,
            true,
            100000,
            9000,
            9000,
            0,
            18000,
            118000,
            Instant.parse("2026-05-15T10:00:00Z"));
    when(invoiceRepo.findByTenantIdAndId("tenant1", "inv1")).thenReturn(Optional.of(issued));

    Invoice sent = service.markSent("inv1", "EMAIL");

    assertThat(sent.getStatus()).isEqualTo("SENT");
    verify(audit).record(eq("INVOICE_SENT"), eq("invoice"), eq("inv1"), any());
    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("INVOICE_SENT");
  }
}
