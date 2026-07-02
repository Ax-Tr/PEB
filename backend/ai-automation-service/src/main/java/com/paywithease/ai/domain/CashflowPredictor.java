package com.paywithease.ai.domain;

import java.util.List;

/**
 * Pure, advisory next-period cashflow forecast from a history of per-period net amounts (paise).
 * Uses ordinary least-squares linear regression for the point estimate and derives a confidence
 * from the fit (R²): a steadier series yields a higher-confidence forecast. The output is always
 * advisory — {@link SuggestionKind#CASHFLOW_FORECAST} is never auto-applied.
 */
public final class CashflowPredictor {

  private CashflowPredictor() {}

  public record Forecast(long projectedNetMinor, double confidence, String basis) {}

  private static final int MIN_HISTORY = 3;

  public static Forecast predict(List<Long> periodNets) {
    if (periodNets == null || periodNets.size() < MIN_HISTORY) {
      return new Forecast(0, 0.0, "Insufficient history (need " + MIN_HISTORY + " periods)");
    }
    int n = periodNets.size();
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumX2 = 0;
    for (int i = 0; i < n; i++) {
      double x = i;
      double y = periodNets.get(i);
      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumX2 += x * x;
    }
    double denom = n * sumX2 - sumX * sumX;
    double slope = denom == 0 ? 0 : (n * sumXY - sumX * sumY) / denom;
    double intercept = (sumY - slope * sumX) / n;
    double projected = slope * n + intercept; // predict the next index (x = n)

    double confidence = rSquared(periodNets, slope, intercept);
    String basis = "OLS trend over " + n + " periods (slope " + Math.round(slope) + "/period)";
    return new Forecast(Math.round(projected), round(confidence), basis);
  }

  private static double rSquared(List<Long> ys, double slope, double intercept) {
    double mean = ys.stream().mapToDouble(Long::doubleValue).average().orElse(0);
    double ssTot = 0;
    double ssRes = 0;
    for (int i = 0; i < ys.size(); i++) {
      double y = ys.get(i);
      double fit = slope * i + intercept;
      ssTot += (y - mean) * (y - mean);
      ssRes += (y - fit) * (y - fit);
    }
    if (ssTot == 0) {
      return 1.0; // perfectly flat series — the trend explains it exactly
    }
    double r2 = 1.0 - ssRes / ssTot;
    return Math.max(0.0, Math.min(1.0, r2));
  }

  private static double round(double v) {
    return Math.round(v * 10000.0) / 10000.0;
  }
}
