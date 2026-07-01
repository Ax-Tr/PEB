package com.paywithease.invoice.domain.gst;

import com.paywithease.common.money.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure GST calculation engine (no framework dependencies) — the correctness core of the invoice
 * service. Rules:
 *
 * <ul>
 *   <li>Intra-state supply (business state == place of supply) → CGST + SGST, each half of the GST
 *       amount; the split is computed so {@code cgst + sgst == totalTax} exactly (no lost paise).
 *   <li>Inter-state supply → IGST (full GST amount).
 *   <li>Tax per line = round(taxableValue × rate%) using banker's rounding to paise.
 *   <li>Bill of supply / exempt / nil-rated (rate 0) → no tax.
 *   <li>Reverse charge → tax is computed and reported but NOT collected on this invoice (payable by
 *       the recipient); it is therefore excluded from the amount payable.
 * </ul>
 *
 * All amounts are integer paise.
 */
public final class GstCalculator {

  private GstCalculator() {}

  /** One taxable line: its net taxable value (qty × price − discount) and its GST rate. */
  public record LineInput(long taxableValueMinor, BigDecimal gstRatePercent) {}

  public record LineTax(
      long taxableValueMinor,
      BigDecimal gstRatePercent,
      long cgstMinor,
      long sgstMinor,
      long igstMinor,
      long totalTaxMinor) {}

  /** Tax grouped by rate — the shape persisted as gst_tax_lines and used in GSTR summaries. */
  public record RateSummary(
      BigDecimal gstRatePercent,
      long taxableValueMinor,
      long cgstMinor,
      long sgstMinor,
      long igstMinor) {}

  public record Result(
      boolean interState,
      boolean reverseCharge,
      List<LineTax> lines,
      List<RateSummary> summaryByRate,
      long totalTaxableMinor,
      long totalCgstMinor,
      long totalSgstMinor,
      long totalIgstMinor,
      long totalTaxMinor,
      long invoiceTotalMinor) {}

  /**
   * @param businessStateCode 2-digit GST state code of the supplier
   * @param placeOfSupplyStateCode 2-digit GST state code of the place of supply
   * @param taxable whether the document attracts GST (false for a Bill of Supply / composition)
   * @param reverseCharge whether GST is payable by the recipient under RCM
   */
  public static Result compute(
      String businessStateCode,
      String placeOfSupplyStateCode,
      List<LineInput> lineInputs,
      boolean taxable,
      boolean reverseCharge) {
    boolean interState = !businessStateCode.equals(placeOfSupplyStateCode);

    List<LineTax> lines = new ArrayList<>();
    long totalTaxable = 0;
    long totalCgst = 0;
    long totalSgst = 0;
    long totalIgst = 0;

    for (LineInput line : lineInputs) {
      long taxableValue = line.taxableValueMinor();
      totalTaxable += taxableValue;

      long cgst = 0;
      long sgst = 0;
      long igst = 0;
      long totalTax = 0;

      boolean rateApplies =
          taxable && line.gstRatePercent() != null && line.gstRatePercent().signum() > 0;
      if (rateApplies) {
        totalTax = Money.ofMinor(taxableValue).percent(line.gstRatePercent()).toMinor();
        if (interState) {
          igst = totalTax;
        } else {
          cgst = totalTax / 2; // floor
          sgst = totalTax - cgst; // remainder → no paise lost, sgst >= cgst
        }
      }

      lines.add(new LineTax(taxableValue, line.gstRatePercent(), cgst, sgst, igst, totalTax));
      totalCgst += cgst;
      totalSgst += sgst;
      totalIgst += igst;
    }

    long totalTax = totalCgst + totalSgst + totalIgst;
    // Under reverse charge the supplier does not collect GST on this invoice.
    long collectedTax = reverseCharge ? 0 : totalTax;
    long invoiceTotal = totalTaxable + collectedTax;

    return new Result(
        interState,
        reverseCharge,
        lines,
        summarize(lines),
        totalTaxable,
        totalCgst,
        totalSgst,
        totalIgst,
        totalTax,
        invoiceTotal);
  }

  private static List<RateSummary> summarize(List<LineTax> lines) {
    Map<BigDecimal, long[]> byRate = new LinkedHashMap<>(); // rate -> [taxable, cgst, sgst, igst]
    for (LineTax line : lines) {
      BigDecimal rate =
          line.gstRatePercent() == null
              ? BigDecimal.ZERO
              : line.gstRatePercent().stripTrailingZeros();
      long[] acc = byRate.computeIfAbsent(rate, r -> new long[4]);
      acc[0] += line.taxableValueMinor();
      acc[1] += line.cgstMinor();
      acc[2] += line.sgstMinor();
      acc[3] += line.igstMinor();
    }
    List<RateSummary> summaries = new ArrayList<>();
    byRate.forEach(
        (rate, acc) -> summaries.add(new RateSummary(rate, acc[0], acc[1], acc[2], acc[3])));
    return summaries;
  }
}
