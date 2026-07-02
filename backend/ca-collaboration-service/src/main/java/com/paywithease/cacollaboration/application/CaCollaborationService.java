package com.paywithease.cacollaboration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.cacollaboration.domain.CaInvite;
import com.paywithease.cacollaboration.domain.CloseChecklist;
import com.paywithease.cacollaboration.domain.CloseChecklistItem;
import com.paywithease.cacollaboration.domain.CollaboratorRole;
import com.paywithease.cacollaboration.domain.ReportApproval;
import com.paywithease.cacollaboration.domain.ReviewNote;
import com.paywithease.cacollaboration.infrastructure.CaInviteRepository;
import com.paywithease.cacollaboration.infrastructure.CloseChecklistItemRepository;
import com.paywithease.cacollaboration.infrastructure.CloseChecklistRepository;
import com.paywithease.cacollaboration.infrastructure.ReportApprovalRepository;
import com.paywithease.cacollaboration.infrastructure.ReviewNoteRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CA / accountant / auditor collaboration workspace: role-scoped invitations (with mid-review
 * revocation), append-only review notes, maker-checker report approvals, and the month-end close
 * checklist that gates the month lock. Auditor read-only enforcement lives at the API layer; this
 * service additionally blocks contributions from collaborators whose access has been revoked.
 */
@Service
public class CaCollaborationService {

  private static final String SOURCE = "ca-collaboration-service";

  private final CaInviteRepository invites;
  private final ReviewNoteRepository notes;
  private final ReportApprovalRepository approvals;
  private final CloseChecklistRepository checklists;
  private final CloseChecklistItemRepository checklistItems;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Duration inviteValidity;

  public CaCollaborationService(
      CaInviteRepository invites,
      ReviewNoteRepository notes,
      ReportApprovalRepository approvals,
      CloseChecklistRepository checklists,
      CloseChecklistItemRepository checklistItems,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.collaboration.invite-validity-days:14}") long inviteValidityDays) {
    this.invites = invites;
    this.notes = notes;
    this.approvals = approvals;
    this.checklists = checklists;
    this.checklistItems = checklistItems;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.inviteValidity = Duration.ofDays(inviteValidityDays);
  }

  // -------------------- Invitations --------------------

  @Transactional
  public CaInvite invite(String email, CollaboratorRole role) {
    CaInvite invite =
        new CaInvite(
            Ulid.newId(),
            TenantContext.requireTenantId(),
            email,
            role,
            actor(),
            clock.instant().plus(inviteValidity),
            clock.instant());
    invites.save(invite);
    audit.record("CA_INVITE_CREATED", "ca_invite", invite.getId(), Map.of("role", role.name()));
    return invite;
  }

  @Transactional
  public CaInvite acceptInvite(String inviteId, String userId) {
    CaInvite invite = loadInvite(inviteId);
    invite.accept(userId, clock.instant());
    invites.save(invite);
    audit.record("CA_INVITE_ACCEPTED", "ca_invite", inviteId, Map.of());
    return invite;
  }

  @Transactional
  public CaInvite revokeInvite(String inviteId) {
    CaInvite invite = loadInvite(inviteId);
    invite.revoke(clock.instant());
    invites.save(invite);
    audit.record("CA_INVITE_REVOKED", "ca_invite", inviteId, Map.of());
    return invite;
  }

  @Transactional(readOnly = true)
  public List<CaInvite> listInvites() {
    return invites.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  // -------------------- Review notes --------------------

  @Transactional
  public ReviewNote addReviewNote(String entityType, String entityId, String note) {
    String tenantId = TenantContext.requireTenantId();
    String author = actor();
    assertCanContribute(tenantId, author);
    ReviewNote reviewNote =
        new ReviewNote(Ulid.newId(), tenantId, entityType, entityId, author, note, clock.instant());
    notes.save(reviewNote);
    audit.record(
        "REVIEW_NOTE_ADDED",
        "review_note",
        reviewNote.getId(),
        Map.of("entityType", entityType, "entityId", entityId));
    return reviewNote;
  }

  @Transactional(readOnly = true)
  public List<ReviewNote> listNotes(String entityType, String entityId) {
    return notes.findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(
        TenantContext.requireTenantId(), entityType, entityId);
  }

  // -------------------- Report approvals (maker-checker) --------------------

  @Transactional
  public ReportApproval requestApproval(String reportType, String reportRef) {
    String tenantId = TenantContext.requireTenantId();
    assertCanContribute(tenantId, actor());
    if (approvals.existsByTenantIdAndReportTypeAndReportRefAndStatus(
        tenantId, reportType, reportRef, "REQUESTED")) {
      throw new ApiException(ErrorCode.CONFLICT, "An approval is already pending for this report");
    }
    ReportApproval approval =
        new ReportApproval(Ulid.newId(), tenantId, reportType, reportRef, actor(), clock.instant());
    approvals.save(approval);
    audit.record(
        "APPROVAL_REQUESTED",
        "report_approval",
        approval.getId(),
        Map.of("reportType", reportType, "reportRef", reportRef));
    emit("APPROVAL_REQUESTED", approval);
    return approval;
  }

  @Transactional
  public ReportApproval decideApproval(String approvalId, boolean approved, String note) {
    String tenantId = TenantContext.requireTenantId();
    assertCanContribute(tenantId, actor());
    ReportApproval approval = loadApproval(approvalId);
    approval.decide(actor(), approved, note, clock.instant());
    approvals.save(approval);
    audit.record(
        "APPROVAL_COMPLETED",
        "report_approval",
        approvalId,
        Map.of("decision", approval.getStatus()));
    emit("APPROVAL_COMPLETED", approval);
    return approval;
  }

  @Transactional(readOnly = true)
  public List<ReportApproval> listApprovals(String reportType, String reportRef) {
    return approvals.findByTenantIdAndReportTypeAndReportRef(
        TenantContext.requireTenantId(), reportType, reportRef);
  }

  // -------------------- Month-end close checklist --------------------

  public record ItemSpec(String label, boolean mandatory) {}

  public record ChecklistView(
      CloseChecklist checklist, List<CloseChecklistItem> items, boolean canLockMonth) {}

  @Transactional
  public ChecklistView createChecklist(int year, int month, List<ItemSpec> specs) {
    String tenantId = TenantContext.requireTenantId();
    if (checklists.findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month).isPresent()) {
      throw new ApiException(
          ErrorCode.CONFLICT, "A close checklist already exists for this period");
    }
    CloseChecklist checklist =
        new CloseChecklist(Ulid.newId(), tenantId, year, month, actor(), clock.instant());
    checklists.save(checklist);
    int order = 0;
    for (ItemSpec spec : specs) {
      checklistItems.save(
          new CloseChecklistItem(
              Ulid.newId(), checklist.getId(), tenantId, spec.label(), spec.mandatory(), order++));
    }
    audit.record(
        "CLOSE_CHECKLIST_CREATED",
        "close_checklist",
        checklist.getId(),
        Map.of("year", year, "month", month, "items", specs.size()));
    return status(checklist);
  }

  @Transactional
  public ChecklistView setChecklistItem(String itemId, boolean done) {
    String tenantId = TenantContext.requireTenantId();
    CloseChecklistItem item =
        checklistItems
            .findByTenantIdAndId(tenantId, itemId)
            .orElseThrow(() -> ApiException.notFound("Checklist item"));
    item.setDone(done, actor(), clock.instant());
    checklistItems.save(item);
    CloseChecklist checklist =
        checklists
            .findByTenantIdAndId(tenantId, item.getChecklistId())
            .orElseThrow(() -> ApiException.notFound("Close checklist"));
    return status(checklist);
  }

  @Transactional(readOnly = true)
  public ChecklistView checklistStatus(String checklistId) {
    CloseChecklist checklist =
        checklists
            .findByTenantIdAndId(TenantContext.requireTenantId(), checklistId)
            .orElseThrow(() -> ApiException.notFound("Close checklist"));
    return status(checklist);
  }

  @Transactional(readOnly = true)
  public ChecklistView checklistForPeriod(int year, int month) {
    CloseChecklist checklist =
        checklists
            .findByTenantIdAndPeriodYearAndPeriodMonth(TenantContext.requireTenantId(), year, month)
            .orElseThrow(() -> ApiException.notFound("Close checklist"));
    return status(checklist);
  }

  private ChecklistView status(CloseChecklist checklist) {
    List<CloseChecklistItem> items =
        checklistItems.findByChecklistIdOrderBySortOrderAsc(checklist.getId());
    return new ChecklistView(checklist, items, CloseChecklist.canLockMonth(items));
  }

  // -------------------- Helpers --------------------

  /**
   * Guards contributions: a collaborator who was invited must currently hold active, non-read-only
   * access. If the actor has no invite record they are an internal user (role is enforced at the
   * API), so they are allowed. This is what blocks a CA whose access was revoked mid-review.
   */
  private void assertCanContribute(String tenantId, String userId) {
    List<CaInvite> userInvites = invites.findByTenantIdAndLinkedUserId(tenantId, userId);
    if (userInvites.isEmpty()) {
      return; // internal user
    }
    boolean hasActiveContributorAccess =
        userInvites.stream()
            .anyMatch(
                i ->
                    i.hasActiveAccess(clock.instant())
                        && !CollaboratorRole.valueOf(i.getRole()).isReadOnly());
    if (!hasActiveContributorAccess) {
      throw new ApiException(
          ErrorCode.FORBIDDEN, "Your collaborator access is not active or is read-only");
    }
  }

  private CaInvite loadInvite(String id) {
    return invites
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Invitation"));
  }

  private ReportApproval loadApproval(String id) {
    return approvals
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Approval"));
  }

  private String actor() {
    return TenantContext.actorId()
        .orElseThrow(
            () -> new ApiException(ErrorCode.UNAUTHENTICATED, "No acting user in context"));
  }

  private void emit(String eventType, ReportApproval approval) {
    var payload =
        objectMapper
            .createObjectNode()
            .put("approvalId", approval.getId())
            .put("reportType", approval.getReportType())
            .put("reportRef", approval.getReportRef())
            .put("status", approval.getStatus())
            .put("requestedBy", approval.getRequestedBy())
            .put("decidedBy", approval.getDecidedBy());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(approval.getTenantId())
            .businessId(approval.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(approval.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
