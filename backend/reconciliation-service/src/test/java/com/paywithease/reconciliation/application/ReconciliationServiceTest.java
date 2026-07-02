package com.paywithease.reconciliation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.reconciliation.domain.ReconItem;
import com.paywithease.reconciliation.domain.ReconSide;
import com.paywithease.reconciliation.infrastructure.ReconExceptionRepository;
import com.paywithease.reconciliation.infrastructure.ReconItemRepository;
import com.paywithease.reconciliation.infrastructure.ReconMatchRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconciliationServiceTest {

  @Mock ReconItemRepository items;
  @Mock ReconMatchRepository matches;
  @Mock ReconExceptionRepository exceptions;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private ReconciliationService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate D = LocalDate.of(2026, 7, 1);

  @BeforeEach
  void setUp() {
    service =
        new ReconciliationService(items, matches, exceptions, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(items.save(any())).thenAnswer(returnsFirstArg());
    when(matches.save(any())).thenAnswer(returnsFirstArg());
    when(exceptions.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private ReconItem ext(String id, long amt, String ref) {
    return new ReconItem(
        id,
        "tenant1",
        ReconSide.EXTERNAL,
        "BANK_TXN",
        "src-" + id,
        "CREDIT",
        amt,
        D,
        ref,
        "Rahul",
        "UTR " + ref,
        clock.instant());
  }

  private ReconItem internal(String id, long amt, String ref) {
    return new ReconItem(
        id,
        "tenant1",
        ReconSide.INTERNAL,
        "PAYMENT",
        "src-" + id,
        "CREDIT",
        amt,
        D,
        ref,
        "Rahul",
        "invoice",
        clock.instant());
  }

  @Test
  void recordItemIsIdempotent() {
    ReconItem existing = ext("e1", 100, "R1");
    when(items.findByTenantIdAndSideAndSourceTypeAndSourceRef(
            "tenant1", "EXTERNAL", "BANK_TXN", "ref1"))
        .thenReturn(Optional.of(existing));
    var cmd =
        new ReconciliationService.ItemCommand(
            "BANK_TXN", "ref1", "CREDIT", 100, D, "R1", "Rahul", "n");
    ReconItem result = service.recordItem(ReconSide.EXTERNAL, cmd);
    assertThat(result).isSameAs(existing);
    verify(items, never()).save(any());
  }

  @Test
  void runAutoMatchesAndMarksReconciled() {
    ReconItem external = ext("e1", 118000, "UTR123");
    ReconItem candidate = internal("i1", 118000, "UTR123");
    when(items.findByTenantIdAndSideAndMatchedFalse("tenant1", "EXTERNAL"))
        .thenReturn(List.of(external));
    when(matches.existsByTenantIdAndExternalItemIdAndStatusNot("tenant1", "e1", "REJECTED"))
        .thenReturn(false);
    when(items.findByTenantIdAndSideAndDirectionAndMatchedFalse("tenant1", "INTERNAL", "CREDIT"))
        .thenReturn(List.of(candidate));
    when(items.findByTenantIdAndId(eq("tenant1"), any()))
        .thenAnswer(
            inv -> {
              String id = inv.getArgument(1);
              return Optional.of(id.equals("e1") ? external : candidate);
            });

    ReconciliationService.RunResult result = service.run();

    assertThat(result.autoMatched()).isEqualTo(1);
    assertThat(external.isMatched()).isTrue();
    assertThat(candidate.isMatched()).isTrue();
    verify(outbox, atLeastOnce()).append(any(EventEnvelope.class)); // RECONCILIATION_MATCHED
  }

  @Test
  void runCreatesExceptionWhenNoCandidate() {
    ReconItem external = ext("e1", 118000, "UTR123");
    when(items.findByTenantIdAndSideAndMatchedFalse("tenant1", "EXTERNAL"))
        .thenReturn(List.of(external));
    when(matches.existsByTenantIdAndExternalItemIdAndStatusNot("tenant1", "e1", "REJECTED"))
        .thenReturn(false);
    when(items.findByTenantIdAndSideAndDirectionAndMatchedFalse("tenant1", "INTERNAL", "CREDIT"))
        .thenReturn(List.of()); // nothing to match
    when(exceptions.existsByTenantIdAndItemId("tenant1", "e1")).thenReturn(false);

    ReconciliationService.RunResult result = service.run();

    assertThat(result.exceptionsCreated()).isEqualTo(1);
    verify(exceptions).save(any());
    verify(outbox, atLeastOnce()).append(any()); // RECONCILIATION_EXCEPTION_CREATED
  }

  @Test
  void runSuggestsWhenMediumConfidence() {
    // amount + date match but no reference and different narration → SUGGESTED (not auto), not
    // marked matched
    ReconItem external = ext("e1", 118000, null);
    ReconItem candidate = internal("i1", 118000, null);
    when(items.findByTenantIdAndSideAndMatchedFalse("tenant1", "EXTERNAL"))
        .thenReturn(List.of(external));
    when(matches.existsByTenantIdAndExternalItemIdAndStatusNot("tenant1", "e1", "REJECTED"))
        .thenReturn(false);
    when(items.findByTenantIdAndSideAndDirectionAndMatchedFalse("tenant1", "INTERNAL", "CREDIT"))
        .thenReturn(List.of(candidate));

    ReconciliationService.RunResult result = service.run();

    assertThat(result.suggested()).isEqualTo(1);
    assertThat(external.isMatched()).isFalse(); // suggested items await confirmation
  }
}
