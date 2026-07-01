package com.paywithease.invoice.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywithease.invoice.domain.DocumentSequence;
import com.paywithease.invoice.domain.DocumentType;
import com.paywithease.invoice.infrastructure.DocumentSequenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceNumberServiceTest {

  @Mock private DocumentSequenceRepository sequences;

  private InvoiceNumberService service;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneOffset.UTC);
    service = new InvoiceNumberService(sequences, clock);
  }

  @Test
  void financialYearAprilStartsNewYear() {
    assertThat(service.financialYear(LocalDate.of(2026, 5, 1))).isEqualTo("2026-27");
    assertThat(service.financialYear(LocalDate.of(2026, 4, 1))).isEqualTo("2026-27");
  }

  @Test
  void financialYearBeforeAprilBelongsToPreviousYear() {
    assertThat(service.financialYear(LocalDate.of(2026, 2, 15))).isEqualTo("2025-26");
    assertThat(service.financialYear(LocalDate.of(2026, 3, 31))).isEqualTo("2025-26");
  }

  @Test
  void nextFormatsNumberAndIncrementsSequence() {
    DocumentSequence seq =
        new DocumentSequence("tenant1", DocumentType.TAX_INVOICE.name(), "2026-27", "INV", 1L);
    when(sequences.lock("tenant1", DocumentType.TAX_INVOICE.name(), "2026-27"))
        .thenReturn(Optional.of(seq));

    String number =
        service.next("tenant1", DocumentType.TAX_INVOICE, "INV", LocalDate.of(2026, 5, 15));

    assertThat(number).isEqualTo("INV/2026-27/00001");
    assertThat(seq.getNextNumber()).isEqualTo(2L);
    verify(sequences).save(any(DocumentSequence.class));
  }
}
