package com.paywithease.analytics.application;

import com.paywithease.analytics.domain.AgingCalculator;
import com.paywithease.analytics.domain.CashflowCalculator;
import com.paywithease.analytics.domain.Freshness;
import com.paywithease.analytics.domain.ProfitCalculator;
import com.paywithease.analytics.domain.ProfitabilityRanker;
import com.paywithease.analytics.infrastructure.FactCashMovement;
import com.paywithease.analytics.infrastructure.FactCashMovementRepository;
import com.paywithease.analytics.infrastructure.FactExpense;
import com.paywithease.analytics.infrastructure.FactExpenseRepository;
import com.paywithease.analytics.infrastructure.FactInvoice;
import com.paywithease.analytics.infrastructure.FactInvoiceRepository;
import com.paywithease.analytics.infrastructure.FactProductSaleRepository;
import com.paywithease.analytics.infrastructure.FactPurchase;
import com.paywithease.analytics.infrastructure.FactPurchaseRepository;
import com.paywithease.analytics.infrastructure.StreamWatermark;
import com.paywithease.analytics.infrastructure.StreamWatermarkRepository;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only analytics over an event-fed read-model. The ingest methods are called by the event
 * consumer to advance the projections; the query methods drive dashboards. This service never
 * queries any OLTP service's database — its only inputs are domain events.
 */
@Service
public class AnalyticsService {

  private final FactInvoiceRepository invoices;
  private final FactPurchaseRepository purchases;
  private final FactExpenseRepository expenses;
  private final FactCashMovementRepository cash;
  private final FactProductSaleRepository products;
  private final StreamWatermarkRepository watermarks;
  private final Clock clock;
  private final Duration staleThreshold;

  public AnalyticsService(
      FactInvoiceRepository invoices,
      FactPurchaseRepository purchases,
      FactExpenseRepository expenses,
      FactCashMovementRepository cash,
      FactProductSaleRepository products,
      StreamWatermarkRepository watermarks,
      Clock clock,
      @Value("${peb.analytics.stale-threshold-seconds:300}") long staleThresholdSeconds) {
    this.invoices = invoices;
    this.purchases = purchases;
    this.expenses = expenses;
    this.cash = cash;
    this.products = products;
    this.watermarks = watermarks;
    this.clock = clock;
    this.staleThreshold = Duration.ofSeconds(staleThresholdSeconds);
  }

  // ---------------------------------------------------------------------------------------------
  // Ingest (called by the event consumer). Each is idempotent on the aggregate's natural key.
  // ---------------------------------------------------------------------------------------------

  @Transactional
  public void ingestInvoice(
      String invoiceId,
      String invoiceNumber,
      String customerId,
      String supplyType,
      long totalMinor,
      long taxMinor,
      LocalDate date) {
    String tenantId = TenantContext.requireTenantId();
    if (invoices.existsByInvoiceId(invoiceId)) {
      return;
    }
    long taxable = Math.max(0, totalMinor - taxMinor);
    invoices.save(
        new FactInvoice(
            invoiceId,
            tenantId,
            invoiceNumber,
            customerId,
            null,
            date,
            supplyType,
            taxable,
            taxMinor,
            totalMinor,
            clock.instant()));
  }

  @Transactional
  public void ingestPurchase(
      String billId,
      String vendorId,
      long netMinor,
      long inputGstMinor,
      long totalMinor,
      LocalDate date) {
    String tenantId = TenantContext.requireTenantId();
    if (purchases.existsByBillId(billId)) {
      return;
    }
    purchases.save(
        new FactPurchase(
            billId,
            tenantId,
            vendorId,
            null,
            date,
            netMinor,
            inputGstMinor,
            totalMinor,
            clock.instant()));
  }

  @Transactional
  public void ingestExpense(String expenseId, String category, long amountMinor, LocalDate date) {
    String tenantId = TenantContext.requireTenantId();
    if (expenses.existsByExpenseId(expenseId)) {
      return;
    }
    expenses.save(
        new FactExpense(expenseId, tenantId, category, date, amountMinor, clock.instant()));
  }

  @Transactional
  public void ingestCashMovement(
      String movementId,
      String direction,
      String source,
      String counterpartyId,
      long amountMinor,
      LocalDate date) {
    String tenantId = TenantContext.requireTenantId();
    if (cash.existsByMovementId(movementId)) {
      return;
    }
    cash.save(
        new FactCashMovement(
            movementId,
            tenantId,
            direction,
            source,
            counterpartyId,
            date,
            amountMinor,
            clock.instant()));
  }

  /** Advance the freshness watermark for a stream after an event has been projected. */
  @Transactional
  public void advanceWatermark(String stream, String eventId) {
    String tenantId = TenantContext.requireTenantId();
    StreamWatermark wm = watermarks.findById(StreamWatermark.idOf(tenantId, stream)).orElse(null);
    if (wm == null) {
      watermarks.save(new StreamWatermark(tenantId, stream, eventId, clock.instant()));
    } else {
      wm.advance(eventId, clock.instant());
      watermarks.save(wm);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Queries (dashboards). All read-only.
  // ---------------------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public ProfitCalculator.Pnl profitAndLoss(int year, int month) {
    String tenantId = TenantContext.requireTenantId();
    long revenue = invoices.sumRevenueMinor(tenantId, year, month);
    long cost = purchases.sumCostMinor(tenantId, year, month);
    long opex = expenses.sumExpenseMinor(tenantId, year, month);
    return ProfitCalculator.compute(revenue, cost, opex);
  }

  @Transactional(readOnly = true)
  public AgingCalculator.Aging receivablesAging(LocalDate asOf) {
    String tenantId = TenantContext.requireTenantId();
    List<AgingCalculator.AgingItem> items =
        invoices.findOutstanding(tenantId).stream()
            .map(f -> new AgingCalculator.AgingItem(f.outstandingMinor(), f.getInvoiceDate()))
            .toList();
    return AgingCalculator.compute(items, asOf);
  }

  @Transactional(readOnly = true)
  public AgingCalculator.Aging payablesAging(LocalDate asOf) {
    String tenantId = TenantContext.requireTenantId();
    List<AgingCalculator.AgingItem> items =
        purchases.findOutstanding(tenantId).stream()
            .map(f -> new AgingCalculator.AgingItem(f.outstandingMinor(), f.getBillDate()))
            .toList();
    return AgingCalculator.compute(items, asOf);
  }

  @Transactional(readOnly = true)
  public CashflowCalculator.Cashflow cashflow() {
    String tenantId = TenantContext.requireTenantId();
    // Merge grouped (period, direction) rows into one PeriodFlow per period, preserving order.
    Map<String, long[]> byPeriod = new LinkedHashMap<>(); // key "year-month" -> [inflow, outflow]
    List<int[]> order = new ArrayList<>();
    for (var row : cash.aggregateByPeriodAndDirection(tenantId)) {
      String key = row.getPeriodYear() + "-" + row.getPeriodMonth();
      long[] flow =
          byPeriod.computeIfAbsent(
              key,
              k -> {
                order.add(new int[] {row.getPeriodYear(), row.getPeriodMonth()});
                return new long[2];
              });
      if ("INFLOW".equals(row.getDirection())) {
        flow[0] += row.getAmountMinor();
      } else {
        flow[1] += row.getAmountMinor();
      }
    }
    List<CashflowCalculator.PeriodFlow> periods = new ArrayList<>();
    for (int[] ym : order) {
      long[] flow = byPeriod.get(ym[0] + "-" + ym[1]);
      periods.add(new CashflowCalculator.PeriodFlow(ym[0], ym[1], flow[0], flow[1]));
    }
    // Opening balance is 0: analytics has no bank-balance feed, so this is cumulative net cashflow.
    return CashflowCalculator.compute(0, periods);
  }

  @Transactional(readOnly = true)
  public List<ProfitabilityRanker.Ranked> productProfitability(int year, int month) {
    String tenantId = TenantContext.requireTenantId();
    List<ProfitabilityRanker.Item> items =
        products.findForPeriod(tenantId, year, month).stream()
            .map(
                p ->
                    new ProfitabilityRanker.Item(
                        p.getProductId(),
                        p.getProductName(),
                        p.getRevenueMinor(),
                        p.getCostMinor()))
            .toList();
    return ProfitabilityRanker.rank(items);
  }

  public record StreamFreshness(String stream, Freshness.Status status) {}

  @Transactional(readOnly = true)
  public List<StreamFreshness> freshness() {
    String tenantId = TenantContext.requireTenantId();
    return watermarks.findByTenantId(tenantId).stream()
        .map(
            wm ->
                new StreamFreshness(
                    wm.getStream(),
                    Freshness.evaluate(wm.getLastProcessedAt(), clock.instant(), staleThreshold)))
        .toList();
  }
}
