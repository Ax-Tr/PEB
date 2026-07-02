package com.paywithease.ai.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Pure, robust anomaly detection over a history of amounts (paise). Uses the median and the median
 * absolute deviation (MAD) to compute a robust z-score, which — unlike mean/standard-deviation — is
 * not itself distorted by the outlier it is trying to detect. Falls back to mean/σ when MAD is
 * zero.
 */
public final class AnomalyDetector {

  private AnomalyDetector() {}

  public enum Severity {
    LOW,
    MEDIUM,
    HIGH
  }

  public record Result(boolean anomaly, double score, Severity severity, String detail) {}

  private static final int MIN_HISTORY = 4;
  private static final double MEDIUM_THRESHOLD = 3.5;
  private static final double HIGH_THRESHOLD = 5.0;

  public static Result evaluate(List<Long> history, long observed) {
    if (history == null || history.size() < MIN_HISTORY) {
      return new Result(
          false, 0.0, Severity.LOW, "Insufficient history to judge (need " + MIN_HISTORY + ")");
    }
    double[] xs = history.stream().mapToDouble(Long::doubleValue).sorted().toArray();
    double median = median(xs);
    double mad = medianAbsoluteDeviation(xs, median);

    double score;
    if (mad > 0) {
      score =
          0.6745 * (observed - median) / mad; // 0.6745 makes MAD comparable to σ for normal data
    } else {
      double mean = Arrays.stream(xs).average().orElse(0);
      double sd = stdDev(xs, mean);
      score = sd > 0 ? (observed - mean) / sd : 0.0;
    }

    double abs = Math.abs(score);
    boolean anomaly = abs >= MEDIUM_THRESHOLD;
    Severity severity =
        abs >= HIGH_THRESHOLD
            ? Severity.HIGH
            : (abs >= MEDIUM_THRESHOLD ? Severity.MEDIUM : Severity.LOW);
    String detail =
        anomaly
            ? "Observed "
                + observed
                + " deviates from median "
                + (long) median
                + " (score "
                + round(score)
                + ")"
            : "Within expected range";
    return new Result(anomaly, round(score), severity, detail);
  }

  private static double median(double[] sorted) {
    int n = sorted.length;
    return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
  }

  private static double medianAbsoluteDeviation(double[] sorted, double median) {
    double[] dev = new double[sorted.length];
    for (int i = 0; i < sorted.length; i++) {
      dev[i] = Math.abs(sorted[i] - median);
    }
    Arrays.sort(dev);
    return median(dev);
  }

  private static double stdDev(double[] xs, double mean) {
    double sumSq = 0;
    for (double x : xs) {
      sumSq += (x - mean) * (x - mean);
    }
    return Math.sqrt(sumSq / xs.length);
  }

  private static double round(double v) {
    return Math.round(v * 10000.0) / 10000.0;
  }
}
