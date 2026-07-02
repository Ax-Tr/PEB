package com.paywithease.privacy.domain;

import java.util.List;

/**
 * Pure builder of an erasure plan for a data principal: for each data category present, what action
 * the platform will take. A full erasure is only possible when every category is deletable — in
 * practice financial/tax/KYC categories force a partial outcome (retain + anonymise), which the
 * plan makes explicit so the response to the data principal is honest.
 */
public final class ErasurePlan {

  private ErasurePlan() {}

  public record Line(
      DataCategory category, RetentionPolicy.Action action, int minRetentionYears, String reason) {}

  public record Plan(List<Line> lines, boolean fullErasurePossible, String summary) {}

  public static Plan build(List<DataCategory> categories) {
    List<Line> lines =
        categories.stream()
            .distinct()
            .map(
                c -> {
                  RetentionPolicy.Outcome o = RetentionPolicy.decide(c);
                  return new Line(c, o.action(), o.minRetentionYears(), o.reason());
                })
            .toList();
    boolean full = lines.stream().allMatch(l -> l.action() == RetentionPolicy.Action.DELETE);
    long retained =
        lines.stream().filter(l -> l.action() == RetentionPolicy.Action.RETAIN_LEGAL_HOLD).count();
    long anonymised =
        lines.stream().filter(l -> l.action() == RetentionPolicy.Action.ANONYMIZE).count();
    long deleted = lines.stream().filter(l -> l.action() == RetentionPolicy.Action.DELETE).count();
    String summary =
        full
            ? "All categories can be fully deleted"
            : deleted
                + " deleted, "
                + anonymised
                + " anonymised, "
                + retained
                + " retained under legal hold (financial/tax/KYC records cannot be deleted)";
    return new Plan(lines, full, summary);
  }
}
