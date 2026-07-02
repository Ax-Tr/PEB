package com.paywithease.employee.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywithease.employee.domain.SalaryRun;
import com.paywithease.employee.domain.SalaryRunLine;
import com.paywithease.employee.domain.payroll.PayrollCalculator;
import com.paywithease.employee.domain.payroll.PayrollRates;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PayslipPdfGeneratorTest {

  private final PayslipPdfGenerator generator = new PayslipPdfGenerator();

  @Test
  void generateProducesValidPdfBytes() {
    SalaryRun run =
        new SalaryRun(
            "run1", "tenant1", 2026, 5, 30, "actor1", Instant.parse("2026-05-31T00:00:00Z"));

    PayrollCalculator.Result result =
        PayrollCalculator.compute(
            new PayrollCalculator.Input(2_000_000L, 1_000_000L, 30, 0, 0, 0, 0, true, true, true),
            PayrollRates.defaults());
    SalaryRunLine line =
        new SalaryRunLine("line1", "tenant1", "run1", "emp1", 2_000_000L, 1_000_000L, 0, result);
    run.addTotals(
        result.totalEarningsMinor(),
        result.netPayMinor(),
        result.statutoryWithheldMinor(),
        result.tdsMinor());

    byte[] bytes = generator.generate(run, line, "Ravi Kumar");

    assertThat(bytes).isNotEmpty();
    assertThat(new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF");
  }
}
