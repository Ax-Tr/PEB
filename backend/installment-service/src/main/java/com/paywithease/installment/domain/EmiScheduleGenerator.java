package com.paywithease.installment.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure EMI schedule generator. Splits a principal (integer paise) into N installments whose amounts
 * sum <b>exactly</b> to the principal — the remainder paise are distributed one-per-EMI to the
 * earliest installments, so no paise is ever lost or invented. Due dates advance by the chosen
 * frequency from the first due date.
 */
public final class EmiScheduleGenerator {

  private EmiScheduleGenerator() {}

  public record PlannedEmi(int emiNumber, LocalDate dueDate, long amountMinor) {}

  public static List<PlannedEmi> generate(
      long principalMinor, int numberOfEmis, LocalDate firstDueDate, Frequency frequency) {
    if (principalMinor <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "principal must be positive");
    }
    if (numberOfEmis < 1) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "numberOfEmis must be at least 1");
    }
    if (firstDueDate == null) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "firstDueDate is required");
    }

    long base = principalMinor / numberOfEmis;
    long remainder = principalMinor - (base * numberOfEmis); // 0 .. numberOfEmis-1

    List<PlannedEmi> emis = new ArrayList<>(numberOfEmis);
    for (int i = 0; i < numberOfEmis; i++) {
      long amount = base + (i < remainder ? 1 : 0); // spread remainder to earliest EMIs
      LocalDate due = frequency.advance(firstDueDate, i);
      emis.add(new PlannedEmi(i + 1, due, amount));
    }
    return emis;
  }
}
