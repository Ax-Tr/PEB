package com.paywithease.analytics.api;

import com.paywithease.analytics.application.AnalyticsService;
import com.paywithease.analytics.domain.AgingCalculator;
import com.paywithease.analytics.domain.CashflowCalculator;
import com.paywithease.analytics.domain.Freshness;
import com.paywithease.analytics.domain.ProfitCalculator;
import com.paywithease.analytics.domain.ProfitabilityRanker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Response DTOs for the read-only /analytics dashboard API. All amounts are in paise (minor). */
public final class AnalyticsDtos {

  private AnalyticsDtos() {}

  // -----------------------------------------------------------------------------------------------
  // P&L
  // -----------------------------------------------------------------------------------------------

  public record PnlResponse(
      long revenueMinor,
      long directCostMinor,
      long operatingExpenseMinor,
      long grossProfitMinor,
      long netProfitMinor,
      BigDecimal grossMarginPct,
      BigDecimal netMarginPct) {

    static PnlResponse from(ProfitCalculator.Pnl p) {
      return new PnlResponse(
          p.revenueMinor(),
          p.directCostMinor(),
          p.operatingExpenseMinor(),
          p.grossProfitMinor(),
          p.netProfitMinor(),
          p.grossMarginPct(),
          p.netMarginPct());
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Aging (receivables / payables)
  // -----------------------------------------------------------------------------------------------

  public record AgingBucketResponse(String bucket, long totalMinor, int count) {

    static AgingBucketResponse from(AgingCalculator.BucketTotal b) {
      return new AgingBucketResponse(b.bucket().name(), b.totalMinor(), b.count());
    }
  }

  public record AgingResponse(
      List<AgingBucketResponse> buckets, long totalOutstandingMinor, int totalCount) {

    static AgingResponse from(AgingCalculator.Aging a) {
      return new AgingResponse(
          a.buckets().stream().map(AgingBucketResponse::from).toList(),
          a.totalOutstandingMinor(),
          a.totalCount());
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Cashflow
  // -----------------------------------------------------------------------------------------------

  public record CashflowPeriodResponse(
      int year,
      int month,
      long inflowMinor,
      long outflowMinor,
      long netMinor,
      long closingBalanceMinor) {

    static CashflowPeriodResponse from(CashflowCalculator.PeriodResult p) {
      return new CashflowPeriodResponse(
          p.year(),
          p.month(),
          p.inflowMinor(),
          p.outflowMinor(),
          p.netMinor(),
          p.closingBalanceMinor());
    }
  }

  public record CashflowResponse(
      long openingBalanceMinor,
      long totalInflowMinor,
      long totalOutflowMinor,
      long netMinor,
      long closingBalanceMinor,
      List<CashflowPeriodResponse> periods) {

    static CashflowResponse from(CashflowCalculator.Cashflow c) {
      return new CashflowResponse(
          c.openingBalanceMinor(),
          c.totalInflowMinor(),
          c.totalOutflowMinor(),
          c.netMinor(),
          c.closingBalanceMinor(),
          c.periods().stream().map(CashflowPeriodResponse::from).toList());
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Product profitability
  // -----------------------------------------------------------------------------------------------

  public record ProductProfitabilityResponse(
      String productId,
      String productName,
      long revenueMinor,
      long costMinor,
      long profitMinor,
      BigDecimal marginPct) {

    static ProductProfitabilityResponse from(ProfitabilityRanker.Ranked r) {
      return new ProductProfitabilityResponse(
          r.productId(),
          r.productName(),
          r.revenueMinor(),
          r.costMinor(),
          r.profitMinor(),
          r.marginPct());
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Freshness (read-model staleness indicator)
  // -----------------------------------------------------------------------------------------------

  public record FreshnessResponse(
      String stream, String state, long lagSeconds, Instant lastProcessedAt) {

    static FreshnessResponse from(AnalyticsService.StreamFreshness f) {
      Freshness.Status s = f.status();
      return new FreshnessResponse(
          f.stream(), s.state().name(), s.lagSeconds(), s.lastProcessedAt());
    }
  }
}
