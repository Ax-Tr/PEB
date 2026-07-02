package com.paywithease.employee.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.Map;

/** Request/response payloads for the salary-run API. */
public final class SalaryRunDtos {

  private SalaryRunDtos() {}

  public record AdjustmentDto(
      int lopDays, long incentivesMinor, long otherDeductionsMinor, long tdsMinor) {}

  public record ProcessRunRequest(
      @Min(2000) int year,
      @Min(1) @Max(12) int month,
      @Positive int workingDays,
      Map<String, AdjustmentDto> adjustments) {}

  public record SalaryRunResponse(
      String id,
      int year,
      int month,
      int workingDays,
      String status,
      long totalEarningsMinor,
      long totalNetMinor,
      long totalStatutoryMinor,
      long totalTdsMinor,
      int employeeCount) {}

  public record SalaryRunLineResponse(
      String id,
      String employeeId,
      long grossMinor,
      long earnedGrossMinor,
      long incentivesMinor,
      long pfMinor,
      long esiMinor,
      long ptMinor,
      long tdsMinor,
      long otherDeductionsMinor,
      long netPayMinor,
      String payslipDocumentId) {}

  public record GeneratePayslipsResponse(int generated) {}
}
