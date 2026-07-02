package com.paywithease.compliance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.compliance.domain.ComplianceReport;
import com.paywithease.compliance.domain.ComplianceReportLine;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Request/response DTOs for the /compliance API. */
public final class ComplianceDtos {

  private ComplianceDtos() {}

  /** Generate (or regenerate a DRAFT) report for a given type and period. */
  public record GenerateReportRequest(
      @NotBlank String type, @Min(2000) @Max(2100) int year, @Min(1) @Max(12) int month) {}

  /** Toggle the "underlying data is reconciled" flag (required before a report can be approved). */
  public record ReconciledRequest(@NotNull Boolean reconciled) {}

  /**
   * Record an EXTERNAL portal/API acknowledgement for a filing. This ONLY records the
   * acknowledgement against an already-approved report; it does not itself file with the tax
   * portal.
   */
  public record FilingRequest(@NotBlank String ackReference) {}

  public record ReportResponse(
      String id,
      String type,
      int year,
      int month,
      String status,
      String displayState,
      boolean dataReconciled,
      long totalTaxableMinor,
      long totalTaxMinor,
      long netPayableMinor,
      List<String> missingFields,
      String ackReference) {}

  public record ReportLineResponse(
      String id, String label, long taxableMinor, long taxMinor, long amountMinor) {}

  static ReportResponse toReport(ComplianceReport r, ObjectMapper objectMapper) {
    return new ReportResponse(
        r.getId(),
        r.getType(),
        r.getYear(),
        r.getMonth(),
        r.getStatus(),
        r.displayState(),
        r.isDataReconciled(),
        r.getTotalTaxableMinor(),
        r.getTotalTaxMinor(),
        r.getNetPayableMinor(),
        parseMissingFields(r.getMissingFields(), objectMapper),
        r.getAckReference());
  }

  static ReportLineResponse toLine(ComplianceReportLine l) {
    return new ReportLineResponse(
        l.getId(), l.getLabel(), l.getTaxableMinor(), l.getTaxMinor(), l.getAmountMinor());
  }

  /** Parses the stored missing-fields JSON array into a List; degrades to empty on any problem. */
  private static List<String> parseMissingFields(String json, ObjectMapper objectMapper) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(
          json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  /**
   * Maps the request's {@code type} string to a {@link
   * com.paywithease.compliance.domain.ReportType}.
   */
  static com.paywithease.compliance.domain.ReportType parseType(String type) {
    if (type == null || !com.paywithease.compliance.domain.ReportType.isValid(type)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown report type: " + type);
    }
    return com.paywithease.compliance.domain.ReportType.valueOf(type);
  }
}
