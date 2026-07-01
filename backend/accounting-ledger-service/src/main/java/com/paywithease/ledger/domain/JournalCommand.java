package com.paywithease.ledger.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A request to post a balanced double-entry journal. Immutable; validated to guarantee the core
 * invariant (Σdebit = Σcredit, ≥2 lines, each line one-sided) before it ever reaches the database.
 */
public record JournalCommand(
    LocalDate entryDate,
    String narration,
    String sourceService,
    String sourceEventId,
    String correlationId,
    String createdBy,
    List<Line> lines) {

  public record Line(String accountCode, long debitMinor, long creditMinor, String narration) {
    public Line {
      if (debitMinor < 0 || creditMinor < 0) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "Line amounts must be non-negative");
      }
      if (debitMinor > 0 && creditMinor > 0) {
        throw new ApiException(
            ErrorCode.VALIDATION_FAILED, "A line is a debit or a credit, not both");
      }
      if (debitMinor == 0 && creditMinor == 0) {
        throw new ApiException(ErrorCode.VALIDATION_FAILED, "A line must have a non-zero amount");
      }
    }
  }

  public JournalCommand {
    lines = List.copyOf(lines);
  }

  /** Validates the double-entry invariant, throwing {@code UNBALANCED_JOURNAL} on violation. */
  public void validateBalanced() {
    if (lines.size() < 2) {
      throw new ApiException(ErrorCode.UNBALANCED_JOURNAL, "A journal needs at least two lines");
    }
    long debit = lines.stream().mapToLong(Line::debitMinor).sum();
    long credit = lines.stream().mapToLong(Line::creditMinor).sum();
    if (debit != credit) {
      throw new ApiException(
          ErrorCode.UNBALANCED_JOURNAL,
          "Debits (" + debit + ") do not equal credits (" + credit + ")");
    }
  }

  public long totalDebitMinor() {
    return lines.stream().mapToLong(Line::debitMinor).sum();
  }

  /** Fluent builder for posting templates. */
  public static Builder builder(LocalDate entryDate, String narration) {
    return new Builder(entryDate, narration);
  }

  public static final class Builder {
    private final LocalDate entryDate;
    private final String narration;
    private String sourceService;
    private String sourceEventId;
    private String correlationId;
    private String createdBy;
    private final List<Line> lines = new ArrayList<>();

    private Builder(LocalDate entryDate, String narration) {
      this.entryDate = entryDate;
      this.narration = narration;
    }

    public Builder source(String service, String eventId) {
      this.sourceService = service;
      this.sourceEventId = eventId;
      return this;
    }

    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder createdBy(String createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder debit(String accountCode, long minor, String narration) {
      if (minor > 0) {
        lines.add(new Line(accountCode, minor, 0, narration));
      }
      return this;
    }

    public Builder credit(String accountCode, long minor, String narration) {
      if (minor > 0) {
        lines.add(new Line(accountCode, 0, minor, narration));
      }
      return this;
    }

    public JournalCommand build() {
      JournalCommand command =
          new JournalCommand(
              entryDate, narration, sourceService, sourceEventId, correlationId, createdBy, lines);
      command.validateBalanced();
      return command;
    }
  }
}
