package com.paywithease.notification.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Plans D-3 / D-1 / D-day reminders for a due date. For each configured offset it computes the send
 * date (dueDate − offset) and keeps only those that are today or in the future — past reminder
 * dates are never scheduled.
 */
public final class ReminderPlanner {

  public static final List<Integer> DEFAULT_OFFSETS = List.of(3, 1, 0);

  private ReminderPlanner() {}

  public record PlannedReminder(int offsetDays, LocalDate sendOn) {}

  public static List<PlannedReminder> plan(
      LocalDate dueDate, List<Integer> offsetDays, LocalDate today) {
    List<PlannedReminder> planned = new ArrayList<>();
    for (int offset : offsetDays) {
      LocalDate sendOn = dueDate.minusDays(offset);
      if (!sendOn.isBefore(today)) { // today or later only
        planned.add(new PlannedReminder(offset, sendOn));
      }
    }
    return planned;
  }
}
