package com.paywithease.analytics.api;

import com.paywithease.analytics.application.AnalyticsService;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Analytics dashboard API — a read-only, event-fed OLAP read-model: P&amp;L / margins, cashflow,
 * receivables/payables aging, product profitability, and a read-model freshness indicator.
 *
 * <p>Every endpoint is a GET and only reads the projection; ingestion is event-driven (see {@code
 * AnalyticsEventConsumer}). Any authenticated tenant user may read. Because the read-model is
 * eventually consistent, {@code GET /freshness} exposes how current each stream is.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@Tag(
    name = "analytics",
    description = "Read-only, event-fed dashboards: profit/margin, cashflow, aging, profitability")
public class AnalyticsController {

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final AnalyticsService service;

  public AnalyticsController(AnalyticsService service) {
    this.service = service;
  }

  @GetMapping("/pnl")
  @Operation(
      summary = "Profit & loss (revenue, cost, opex, gross/net profit and margins) for a period")
  public AnalyticsDtos.PnlResponse pnl(@RequestParam int year, @RequestParam int month) {
    validatePeriod(year, month);
    return AnalyticsDtos.PnlResponse.from(service.profitAndLoss(year, month));
  }

  @GetMapping("/receivables-aging")
  @Operation(summary = "Receivables aging buckets as of a date (defaults to today, Asia/Kolkata)")
  public AnalyticsDtos.AgingResponse receivablesAging(@RequestParam(required = false) String asOf) {
    return AnalyticsDtos.AgingResponse.from(service.receivablesAging(parseAsOf(asOf)));
  }

  @GetMapping("/payables-aging")
  @Operation(summary = "Payables aging buckets as of a date (defaults to today, Asia/Kolkata)")
  public AnalyticsDtos.AgingResponse payablesAging(@RequestParam(required = false) String asOf) {
    return AnalyticsDtos.AgingResponse.from(service.payablesAging(parseAsOf(asOf)));
  }

  @GetMapping("/cashflow")
  @Operation(summary = "Per-period cashflow (inflows/outflows, net, running closing balance)")
  public AnalyticsDtos.CashflowResponse cashflow() {
    return AnalyticsDtos.CashflowResponse.from(service.cashflow());
  }

  @GetMapping("/product-profitability")
  @Operation(
      summary = "Products ranked by profit for a period (empty until invoice line detail flows)")
  public List<AnalyticsDtos.ProductProfitabilityResponse> productProfitability(
      @RequestParam int year, @RequestParam int month) {
    validatePeriod(year, month);
    return service.productProfitability(year, month).stream()
        .map(AnalyticsDtos.ProductProfitabilityResponse::from)
        .toList();
  }

  @GetMapping("/commitments-summary")
  @Operation(summary = "Commitment operational summary from analytics read-model")
  public AnalyticsDtos.CommitmentSummaryResponse commitmentsSummary(
      @RequestParam(required = false) String asOf) {
    return AnalyticsDtos.CommitmentSummaryResponse.from(
        service.commitmentsSummary(parseAsOf(asOf)));
  }

  @GetMapping("/collection-efficiency")
  @Operation(summary = "Promise-to-collection conversion across tracked commitments")
  public AnalyticsDtos.CollectionEfficiencyResponse collectionEfficiency() {
    return AnalyticsDtos.CollectionEfficiencyResponse.from(service.collectionEfficiency());
  }

  @GetMapping("/broken-promises")
  @Operation(summary = "Broken promises requiring owner follow-up")
  public List<AnalyticsDtos.CommitmentItemResponse> brokenPromises() {
    return service.brokenPromises().stream()
        .map(AnalyticsDtos.CommitmentItemResponse::from)
        .toList();
  }

  @GetMapping("/upcoming-obligations")
  @Operation(summary = "Upcoming commitments and obligations due soon")
  public List<AnalyticsDtos.CommitmentItemResponse> upcomingObligations(
      @RequestParam(required = false) String from, @RequestParam(defaultValue = "7") int days) {
    return service.upcomingObligations(parseAsOf(from), Math.min(Math.max(days, 0), 90)).stream()
        .map(AnalyticsDtos.CommitmentItemResponse::from)
        .toList();
  }

  @GetMapping("/freshness")
  @Operation(summary = "Read-model freshness per stream (analytics is eventually consistent)")
  public List<AnalyticsDtos.FreshnessResponse> freshness() {
    return service.freshness().stream().map(AnalyticsDtos.FreshnessResponse::from).toList();
  }

  private static void validatePeriod(int year, int month) {
    if (year < 2000 || year > 2100) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "year must be between 2000 and 2100");
    }
    if (month < 1 || month > 12) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "month must be between 1 and 12");
    }
  }

  /** asOf is optional; defaults to today (Asia/Kolkata). Bad ISO date → 400 VALIDATION_FAILED. */
  private static LocalDate parseAsOf(String asOf) {
    if (asOf == null || asOf.isBlank()) {
      return LocalDate.now(IST);
    }
    try {
      return LocalDate.parse(asOf);
    } catch (DateTimeParseException e) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "asOf must be an ISO date (YYYY-MM-DD)");
    }
  }
}
