package com.paywithease.employee.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Request/response DTOs for the /employees API. Monetary amounts are integer paise ({@code
 * *Minor}).
 */
public final class EmployeeDtos {

  private EmployeeDtos() {}

  public record CreateEmployee(
      @NotBlank String name,
      @Pattern(regexp = "(\\+?91)?[6-9]\\d{9}", message = "invalid Indian mobile number")
          String mobile,
      String email,
      String pan,
      String designation,
      LocalDate dateOfJoining) {}

  public record EmployeeResponse(
      String id,
      String name,
      String mobile,
      String email,
      String pan,
      String designation,
      LocalDate dateOfJoining,
      String status) {}

  public record SalaryStructureRequest(
      @Positive long grossSalaryMinor,
      @PositiveOrZero long basicMinor,
      @PositiveOrZero long hraMinor,
      boolean pfApplicable,
      boolean esiApplicable,
      boolean ptApplicable,
      LocalDate effectiveFrom) {}

  public record SalaryStructureResponse(
      String employeeId,
      long grossSalaryMinor,
      long basicMinor,
      long hraMinor,
      boolean pfApplicable,
      boolean esiApplicable,
      boolean ptApplicable,
      LocalDate effectiveFrom) {}
}
