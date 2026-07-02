package com.paywithease.compliance.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure aggregation of period source records into a compliance report's lines + totals, with
 * missing-data detection. These are <b>preparation</b> summaries: GSTR-1/3B summaries,
 * sales/purchase registers, ITC, payroll (PF/ESI/PT), TDS, and an ITR-ready P&amp;L-style summary.
 * Rate/HSN line-level GSTR-1 detail requires enriched invoice events (flagged as a follow-up).
 */
public final class ReportBuilder {

  private ReportBuilder() {}

  /** A minimal projection of a source record for aggregation. */
  public record SourceView(
      String recordType,
      long taxableMinor,
      long taxMinor,
      long statutoryMinor,
      long tdsMinor,
      String supplyType) {}

  public record Line(String label, long taxableMinor, long taxMinor, long amountMinor) {}

  public record Built(
      long totalTaxableMinor,
      long totalTaxMinor,
      long netPayableMinor,
      List<Line> lines,
      List<String> missingFields) {}

  public static Built build(ReportType type, List<SourceView> records) {
    return switch (type) {
      case SALES_REGISTER, GSTR1_SUMMARY -> salesSide(records);
      case PURCHASE_REGISTER, ITC_SUMMARY -> purchaseSide(records);
      case GSTR3B_SUMMARY -> gstr3b(records);
      case PAYROLL_COMPLIANCE -> payroll(records);
      case TDS_SUMMARY -> tds(records);
      case ITR_SUMMARY -> itr(records);
    };
  }

  private static Built salesSide(List<SourceView> records) {
    long b2bTaxable = sum(records, "SALES", "B2B", true);
    long b2bTax = sum(records, "SALES", "B2B", false);
    long b2cTaxable = sum(records, "SALES", "B2C", true);
    long b2cTax = sum(records, "SALES", "B2C", false);
    List<Line> lines = new ArrayList<>();
    lines.add(new Line("B2B sales", b2bTaxable, b2bTax, b2bTaxable + b2bTax));
    lines.add(new Line("B2C sales", b2cTaxable, b2cTax, b2cTaxable + b2cTax));
    long taxable = b2bTaxable + b2cTaxable;
    long tax = b2bTax + b2cTax;
    List<String> missing = new ArrayList<>();
    if (taxable == 0 && tax == 0) {
      missing.add("No sales transactions recorded for this period");
    }
    return new Built(taxable, tax, tax, lines, missing);
  }

  private static Built purchaseSide(List<SourceView> records) {
    long taxable = sumType(records, "PURCHASE", true);
    long itc = sumType(records, "PURCHASE", false);
    List<Line> lines = List.of(new Line("Purchases + ITC", taxable, itc, taxable + itc));
    List<String> missing = new ArrayList<>();
    if (taxable == 0 && itc == 0) {
      missing.add("No purchases recorded for this period");
    }
    return new Built(taxable, itc, itc, lines, missing);
  }

  private static Built gstr3b(List<SourceView> records) {
    long outputTax = sumType(records, "SALES", false);
    long salesTaxable = sumType(records, "SALES", true);
    long itc = sumType(records, "PURCHASE", false);
    long netPayable = Math.max(0, outputTax - itc);
    List<Line> lines =
        List.of(
            new Line("Output GST (on sales)", salesTaxable, outputTax, outputTax),
            new Line("Input Tax Credit (ITC)", 0, itc, itc),
            new Line("Net GST payable", 0, netPayable, netPayable));
    List<String> missing = new ArrayList<>();
    if (outputTax == 0 && itc == 0) {
      missing.add("No GST activity for this period");
    }
    if (itc > outputTax) {
      missing.add("ITC exceeds output tax — carry-forward/refund review needed");
    }
    return new Built(salesTaxable, outputTax, netPayable, lines, missing);
  }

  private static Built payroll(List<SourceView> records) {
    long statutory =
        records.stream()
            .filter(r -> r.recordType().equals("PAYROLL"))
            .mapToLong(SourceView::statutoryMinor)
            .sum();
    long tds =
        records.stream()
            .filter(r -> r.recordType().equals("PAYROLL"))
            .mapToLong(SourceView::tdsMinor)
            .sum();
    List<Line> lines =
        List.of(
            new Line("PF / ESI / PT withheld", 0, statutory, statutory),
            new Line("Salary TDS", 0, tds, tds));
    List<String> missing = new ArrayList<>();
    if (statutory == 0 && tds == 0) {
      missing.add("No payroll runs recorded for this period");
    }
    return new Built(0, statutory + tds, statutory + tds, lines, missing);
  }

  private static Built tds(List<SourceView> records) {
    long salaryTds =
        records.stream()
            .filter(r -> r.recordType().equals("PAYROLL"))
            .mapToLong(SourceView::tdsMinor)
            .sum();
    List<Line> lines = List.of(new Line("Salary TDS", 0, salaryTds, salaryTds));
    List<String> missing = new ArrayList<>();
    missing.add("Vendor/contractor TDS is not yet tracked — include before filing");
    return new Built(0, salaryTds, salaryTds, lines, missing);
  }

  private static Built itr(List<SourceView> records) {
    long revenue = sumType(records, "SALES", true);
    long purchases = sumType(records, "PURCHASE", true);
    long payrollCost =
        records.stream()
            .filter(r -> r.recordType().equals("PAYROLL"))
            .mapToLong(SourceView::statutoryMinor)
            .sum();
    long grossProfit = revenue - purchases;
    List<Line> lines =
        List.of(
            new Line("Revenue (net of GST)", revenue, 0, revenue),
            new Line("Purchases (net of GST)", purchases, 0, purchases),
            new Line("Gross profit (indicative)", 0, 0, grossProfit));
    List<String> missing = new ArrayList<>();
    missing.add("ITR-ready summary is indicative; depreciation and other heads need CA review");
    return new Built(revenue, 0, grossProfit, lines, missing);
  }

  private static long sum(List<SourceView> records, String type, String supply, boolean taxable) {
    return records.stream()
        .filter(r -> r.recordType().equals(type))
        .filter(r -> supply.equals(r.supplyType()))
        .mapToLong(r -> taxable ? r.taxableMinor() : r.taxMinor())
        .sum();
  }

  private static long sumType(List<SourceView> records, String type, boolean taxable) {
    return records.stream()
        .filter(r -> r.recordType().equals(type))
        .mapToLong(r -> taxable ? r.taxableMinor() : r.taxMinor())
        .sum();
  }
}
