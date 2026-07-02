package com.paywithease.reconciliation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.reconciliation.domain.MatchEngine;
import com.paywithease.reconciliation.domain.ReconException;
import com.paywithease.reconciliation.domain.ReconItem;
import com.paywithease.reconciliation.domain.ReconMatch;
import com.paywithease.reconciliation.domain.ReconSide;
import com.paywithease.reconciliation.infrastructure.ReconExceptionRepository;
import com.paywithease.reconciliation.infrastructure.ReconItemRepository;
import com.paywithease.reconciliation.infrastructure.ReconMatchRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs weighted reconciliation: normalizes items from both sides, pairs each unmatched external
 * item with the best internal candidate (AUTO / SUGGESTED / EXCEPTION), and records every decision.
 * Suggested matches await user confirm/reject; unmatched items become exceptions. AUTO/CONFIRMED
 * matches mark both items reconciled so they aren't re-processed.
 */
@Service
public class ReconciliationService {

  private static final String SOURCE = "reconciliation-service";

  private final ReconItemRepository items;
  private final ReconMatchRepository matches;
  private final ReconExceptionRepository exceptions;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ReconciliationService(
      ReconItemRepository items,
      ReconMatchRepository matches,
      ReconExceptionRepository exceptions,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.items = items;
    this.matches = matches;
    this.exceptions = exceptions;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public record ItemCommand(
      String sourceType,
      String sourceRef,
      String direction,
      long amountMinor,
      LocalDate itemDate,
      String reference,
      String counterparty,
      String narration) {}

  /** Records a reconcilable item from an upstream event (idempotent per source ref). */
  @Transactional
  public ReconItem recordItem(ReconSide side, ItemCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    var existing =
        items.findByTenantIdAndSideAndSourceTypeAndSourceRef(
            tenantId, side.name(), cmd.sourceType(), cmd.sourceRef());
    if (existing.isPresent()) {
      return existing.get(); // already recorded — idempotent
    }
    ReconItem item =
        new ReconItem(
            Ulid.newId(),
            tenantId,
            side,
            cmd.sourceType(),
            cmd.sourceRef(),
            cmd.direction(),
            cmd.amountMinor(),
            cmd.itemDate(),
            cmd.reference(),
            cmd.counterparty(),
            cmd.narration(),
            clock.instant());
    return items.save(item);
  }

  public record RunResult(int autoMatched, int suggested, int exceptionsCreated) {}

  /** Matches all unmatched external items against internal candidates. */
  @Transactional
  public RunResult run() {
    String tenantId = TenantContext.requireTenantId();
    Instant now = clock.instant();
    int auto = 0;
    int suggested = 0;
    int exc = 0;

    for (ReconItem external :
        items.findByTenantIdAndSideAndMatchedFalse(tenantId, ReconSide.EXTERNAL.name())) {
      if (matches.existsByTenantIdAndExternalItemIdAndStatusNot(
          tenantId, external.getId(), "REJECTED")) {
        continue; // already has a live proposal/confirmation
      }
      List<ReconItem> candidates =
          items.findByTenantIdAndSideAndDirectionAndMatchedFalse(
              tenantId, ReconSide.INTERNAL.name(), external.getDirection());
      MatchEngine.Result result =
          MatchEngine.match(
              external.toMatchItem(), candidates.stream().map(ReconItem::toMatchItem).toList());

      switch (result.decision()) {
        case AUTO -> {
          createMatch(tenantId, external, result.candidateId(), result.score(), "AUTO", null, now);
          auto++;
        }
        case SUGGESTED -> {
          createMatch(
              tenantId, external, result.candidateId(), result.score(), "SUGGESTED", null, now);
          suggested++;
        }
        case EXCEPTION -> {
          if (!exceptions.existsByTenantIdAndItemId(tenantId, external.getId())) {
            ReconException e =
                new ReconException(
                    Ulid.newId(), tenantId, external.getId(), "No confident internal match", now);
            exceptions.save(e);
            emit(
                "RECONCILIATION_EXCEPTION_CREATED",
                external.getId(),
                Map.of("reason", e.getReason()));
            exc++;
          }
        }
      }
    }
    audit.record(
        "RECONCILIATION_RUN",
        "reconciliation",
        tenantId,
        Map.of("auto", auto, "suggested", suggested, "exceptions", exc));
    return new RunResult(auto, suggested, exc);
  }

  @Transactional
  public ReconMatch confirmMatch(String matchId, String actorId) {
    String tenantId = TenantContext.requireTenantId();
    ReconMatch match =
        matches
            .findByTenantIdAndId(tenantId, matchId)
            .orElseThrow(() -> ApiException.notFound("Match"));
    match.confirm(actorId, clock.instant());
    matches.save(match);
    markMatched(tenantId, match);
    emit(
        "RECONCILIATION_MATCHED",
        match.getExternalItemId(),
        Map.of("matchId", matchId, "status", "CONFIRMED"));
    audit.record("RECONCILIATION_CONFIRMED", "reconciliation_match", matchId, Map.of());
    return match;
  }

  @Transactional
  public ReconMatch rejectMatch(String matchId, String actorId) {
    String tenantId = TenantContext.requireTenantId();
    ReconMatch match =
        matches
            .findByTenantIdAndId(tenantId, matchId)
            .orElseThrow(() -> ApiException.notFound("Match"));
    match.reject(actorId, clock.instant());
    matches.save(match);
    audit.record("RECONCILIATION_REJECTED", "reconciliation_match", matchId, Map.of());
    return match;
  }

  @Transactional
  public ReconMatch manualMatch(String externalItemId, String internalItemId, String actorId) {
    String tenantId = TenantContext.requireTenantId();
    ReconItem external = requireUnmatched(tenantId, externalItemId, ReconSide.EXTERNAL);
    ReconItem internal = requireUnmatched(tenantId, internalItemId, ReconSide.INTERNAL);
    Instant now = clock.instant();
    ReconMatch match =
        new ReconMatch(
            Ulid.newId(),
            tenantId,
            external.getId(),
            internal.getId(),
            BigDecimal.ONE,
            "CONFIRMED",
            actorId,
            now);
    matches.save(match);
    markMatched(tenantId, match);
    emit(
        "RECONCILIATION_MATCHED",
        external.getId(),
        Map.of("matchId", match.getId(), "status", "MANUAL"));
    audit.record("RECONCILIATION_MANUAL_MATCH", "reconciliation_match", match.getId(), Map.of());
    return match;
  }

  @Transactional(readOnly = true)
  public List<ReconMatch> suggestions() {
    return matches.findByTenantIdAndStatusOrderByCreatedAtDesc(
        TenantContext.requireTenantId(), "SUGGESTED");
  }

  @Transactional(readOnly = true)
  public List<ReconException> openExceptions() {
    return exceptions.findByTenantIdAndStatusOrderByCreatedAtDesc(
        TenantContext.requireTenantId(), "OPEN");
  }

  private void createMatch(
      String tenantId,
      ReconItem external,
      String internalId,
      double score,
      String status,
      String actor,
      Instant now) {
    BigDecimal s = BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_EVEN);
    ReconMatch match =
        new ReconMatch(Ulid.newId(), tenantId, external.getId(), internalId, s, status, actor, now);
    matches.save(match);
    if (match.isEffective()) {
      markMatched(tenantId, match);
      emit(
          "RECONCILIATION_MATCHED",
          external.getId(),
          Map.of("matchId", match.getId(), "status", status, "score", s));
    }
  }

  private void markMatched(String tenantId, ReconMatch match) {
    items
        .findByTenantIdAndId(tenantId, match.getExternalItemId())
        .ifPresent(i -> i.markMatched(match.getId()));
    items
        .findByTenantIdAndId(tenantId, match.getInternalItemId())
        .ifPresent(i -> i.markMatched(match.getId()));
  }

  private ReconItem requireUnmatched(String tenantId, String itemId, ReconSide side) {
    ReconItem item =
        items
            .findByTenantIdAndId(tenantId, itemId)
            .orElseThrow(() -> ApiException.notFound("Item"));
    if (!item.getSide().equals(side.name())) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Item is not on the " + side + " side");
    }
    if (item.isMatched()) {
      throw new ApiException(ErrorCode.CONFLICT, "Item already matched");
    }
    return item;
  }

  private void emit(String eventType, String aggregateId, Map<String, ?> data) {
    ObjectNode payload = objectMapper.valueToTree(data);
    payload.put("aggregateId", aggregateId);
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(TenantContext.requireTenantId())
            .businessId(TenantContext.requireTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
