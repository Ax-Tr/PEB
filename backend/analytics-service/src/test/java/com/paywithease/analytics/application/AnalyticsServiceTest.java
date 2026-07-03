package com.paywithease.analytics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywithease.analytics.domain.AgingCalculator;
import com.paywithease.analytics.domain.Freshness;
import com.paywithease.analytics.infrastructure.FactCashMovementRepository;
import com.paywithease.analytics.infrastructure.FactCommitment;
import com.paywithease.analytics.infrastructure.FactCommitmentRepository;
import com.paywithease.analytics.infrastructure.FactExpenseRepository;
import com.paywithease.analytics.infrastructure.FactInvoice;
import com.paywithease.analytics.infrastructure.FactInvoiceRepository;
import com.paywithease.analytics.infrastructure.FactProductSaleRepository;
import com.paywithease.analytics.infrastructure.FactPurchaseRepository;
import com.paywithease.analytics.infrastructure.StreamWatermark;
import com.paywithease.analytics.infrastructure.StreamWatermarkRepository;
import com.paywithease.common.tenant.TenantContext;
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
class AnalyticsServiceTest {

  @Mock FactInvoiceRepository invoices;
  @Mock FactPurchaseRepository purchases;
  @Mock FactExpenseRepository expenses;
  @Mock FactCashMovementRepository cash;
  @Mock FactProductSaleRepository products;
  @Mock FactCommitmentRepository commitments;
  @Mock StreamWatermarkRepository watermarks;

  private AnalyticsService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new AnalyticsService(
            invoices, purchases, expenses, cash, products, commitments, watermarks, clock, 300);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(invoices.save(any())).thenAnswer(returnsFirstArg());
    when(purchases.save(any())).thenAnswer(returnsFirstArg());
    when(expenses.save(any())).thenAnswer(returnsFirstArg());
    when(cash.save(any())).thenAnswer(returnsFirstArg());
    when(commitments.save(any())).thenAnswer(returnsFirstArg());
    when(watermarks.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void ingestInvoiceIsIdempotent() {
    when(invoices.existsByInvoiceId("inv1")).thenReturn(true);
    service.ingestInvoice(
        "inv1", "INV-1", "cust1", "B2B", 118_000, 18_000, LocalDate.of(2026, 5, 10));
    verify(invoices, never()).save(any());
  }

  @Test
  void ingestInvoicePersistsTaxableNetOfGst() {
    when(invoices.existsByInvoiceId("inv2")).thenReturn(false);
    service.ingestInvoice(
        "inv2", "INV-2", "cust1", "B2B", 118_000, 18_000, LocalDate.of(2026, 5, 10));
    verify(invoices).save(any(FactInvoice.class));
  }

  @Test
  void profitAndLossComposesRevenueCostOpex() {
    when(invoices.sumRevenueMinor("tenant1", 2026, 5)).thenReturn(1_000_000L);
    when(purchases.sumCostMinor("tenant1", 2026, 5)).thenReturn(600_000L);
    when(expenses.sumExpenseMinor("tenant1", 2026, 5)).thenReturn(150_000L);

    var pnl = service.profitAndLoss(2026, 5);

    assertThat(pnl.grossProfitMinor()).isEqualTo(400_000);
    assertThat(pnl.netProfitMinor()).isEqualTo(250_000);
  }

  @Test
  void receivablesAgingBucketsOutstandingInvoices() {
    FactInvoice inv =
        new FactInvoice(
            "inv1",
            "tenant1",
            "INV-1",
            "cust1",
            null,
            LocalDate.of(2026, 5, 1),
            "B2B",
            100_000,
            0,
            100_000,
            clock.instant());
    when(invoices.findOutstanding("tenant1")).thenReturn(List.of(inv));

    var aging = service.receivablesAging(LocalDate.of(2026, 7, 1)); // 61 days -> DAYS_61_90

    assertThat(aging.totalOutstandingMinor()).isEqualTo(100_000);
    assertThat(
            aging.buckets().stream()
                .filter(b -> b.bucket() == AgingCalculator.Bucket.DAYS_61_90)
                .findFirst()
                .orElseThrow()
                .totalMinor())
        .isEqualTo(100_000);
  }

  @Test
  void cashflowMergesInflowAndOutflowPerPeriod() {
    when(cash.aggregateByPeriodAndDirection("tenant1"))
        .thenReturn(
            List.of(
                sum(2026, 5, "INFLOW", 500_000),
                sum(2026, 5, "OUTFLOW", 300_000),
                sum(2026, 6, "INFLOW", 200_000)));

    var cf = service.cashflow();

    assertThat(cf.periods()).hasSize(2);
    assertThat(cf.periods().get(0).netMinor()).isEqualTo(200_000);
    assertThat(cf.closingBalanceMinor()).isEqualTo(400_000);
  }

  @Test
  void freshnessReportsStalePerStream() {
    StreamWatermark wm =
        new StreamWatermark(
            "tenant1", "invoice.events", "e1", Instant.parse("2026-06-30T23:00:00Z"));
    when(watermarks.findByTenantId("tenant1")).thenReturn(List.of(wm));

    var fresh = service.freshness();

    assertThat(fresh).hasSize(1);
    assertThat(fresh.get(0).stream()).isEqualTo("invoice.events");
    assertThat(fresh.get(0).status().state()).isEqualTo(Freshness.State.STALE); // 1h > 300s
  }

  @Test
  void ingestCommitmentCreatesOrUpdatesProjection() {
    service.ingestCommitment(
        "c1",
        "CUSTOMER",
        "cust1",
        "Raj",
        "VOICE",
        LocalDate.of(2026, 7, 3),
        500_000,
        0,
        500_000,
        "PROMISED");

    verify(commitments).save(any(FactCommitment.class));
  }

  @Test
  void commitmentSummaryAggregatesDueAndBroken() {
    FactCommitment due = commitment("c1", LocalDate.of(2026, 7, 1), 100_000, "PROMISED");
    FactCommitment overdue = commitment("c2", LocalDate.of(2026, 6, 30), 200_000, "BROKEN");
    when(commitments.countOpen("tenant1")).thenReturn(2L);
    when(commitments.countByTenantIdAndStatus("tenant1", "BROKEN")).thenReturn(1L);
    when(commitments.sumOpenOutstandingMinor("tenant1")).thenReturn(300_000L);
    when(commitments.dueToday("tenant1", LocalDate.of(2026, 7, 1))).thenReturn(List.of(due));
    when(commitments.overdue("tenant1", LocalDate.of(2026, 7, 1))).thenReturn(List.of(overdue));
    when(commitments.dueBetween("tenant1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 8)))
        .thenReturn(List.of(due));

    var summary = service.commitmentsSummary(LocalDate.of(2026, 7, 1));

    assertThat(summary.dueTodayMinor()).isEqualTo(100_000);
    assertThat(summary.overdueMinor()).isEqualTo(200_000);
    assertThat(summary.brokenCount()).isEqualTo(1);
  }

  @Test
  void collectionEfficiencyComputesConversionPct() {
    when(commitments.sumPromisedMinor("tenant1")).thenReturn(1_000_000L);
    when(commitments.sumPaidMinor("tenant1")).thenReturn(650_000L);

    var efficiency = service.collectionEfficiency();

    assertThat(efficiency.conversionPct()).isEqualByComparingTo("65.00");
  }

  @Test
  void advanceWatermarkCreatesThenUpdates() {
    when(watermarks.findById("tenant1|invoice.events")).thenReturn(Optional.empty());
    service.advanceWatermark("invoice.events", "e1");
    verify(watermarks).save(any(StreamWatermark.class));
  }

  private static FactCashMovementRepository.PeriodDirectionSum sum(
      int year, int month, String direction, long amount) {
    return new FactCashMovementRepository.PeriodDirectionSum() {
      @Override
      public int getPeriodYear() {
        return year;
      }

      @Override
      public int getPeriodMonth() {
        return month;
      }

      @Override
      public String getDirection() {
        return direction;
      }

      @Override
      public long getAmountMinor() {
        return amount;
      }
    };
  }

  private static FactCommitment commitment(
      String id, LocalDate dueDate, long outstandingMinor, String status) {
    return new FactCommitment(
        id,
        "tenant1",
        "CUSTOMER",
        "cust1",
        "Raj",
        "MANUAL",
        dueDate,
        outstandingMinor,
        0,
        outstandingMinor,
        status,
        Instant.parse("2026-07-01T00:00:00Z"));
  }
}
