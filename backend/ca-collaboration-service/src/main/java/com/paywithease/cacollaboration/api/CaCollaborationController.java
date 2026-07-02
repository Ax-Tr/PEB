package com.paywithease.cacollaboration.api;

import com.paywithease.cacollaboration.application.CaCollaborationService;
import com.paywithease.cacollaboration.domain.CollaboratorRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CA / accountant / auditor collaboration API — role-scoped external invitations (with mid-review
 * revocation), append-only review notes, maker-checker report approvals, and the month-end close
 * checklist whose {@code canLockMonth} flag gates the ledger month lock.
 *
 * <p>Maker-checker uses DISTINCT authorities: a maker (accountant/CA/owner) requests an approval
 * and a checker (owner/co-owner) decides it; the domain additionally enforces approver !=
 * requester. The AUDITOR collaborator scope is read-only and is enforced at the service layer
 * (there is no AUDITOR identity role), so contribution endpoints do not fabricate an AUDITOR role
 * in {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/v1/collaboration")
@Tag(
    name = "collaboration",
    description =
        "CA/accountant/auditor collaboration: invites, review notes, maker-checker approvals,"
            + " month-end close checklist")
public class CaCollaborationController {

  private final CaCollaborationService service;

  public CaCollaborationController(CaCollaborationService service) {
    this.service = service;
  }

  // -------------------- Invitations --------------------

  @PostMapping("/invites")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER')")
  @Operation(summary = "Invite an external collaborator (accountant / CA / auditor)")
  public CaCollaborationDtos.InviteResponse invite(
      @Valid @RequestBody CaCollaborationDtos.InviteRequest body) {
    CaCollaborationDtos.requireValidRole(body.role());
    return CaCollaborationDtos.toInvite(
        service.invite(body.email(), CollaboratorRole.valueOf(body.role())));
  }

  @PostMapping("/invites/{id}/accept")
  @Operation(summary = "Accept an invitation (the invitee links their user account)")
  public CaCollaborationDtos.InviteResponse accept(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return CaCollaborationDtos.toInvite(service.acceptInvite(id, jwt.getSubject()));
  }

  @PostMapping("/invites/{id}/revoke")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER')")
  @Operation(summary = "Revoke an invitation (allowed even mid-review)")
  public CaCollaborationDtos.InviteResponse revoke(@PathVariable String id) {
    return CaCollaborationDtos.toInvite(service.revokeInvite(id));
  }

  @GetMapping("/invites")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER')")
  @Operation(summary = "List invitations for the tenant (newest first)")
  public List<CaCollaborationDtos.InviteResponse> listInvites() {
    return service.listInvites().stream().map(CaCollaborationDtos::toInvite).toList();
  }

  // -------------------- Review notes --------------------

  @PostMapping("/notes")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(
      summary =
          "Add an append-only review note. Read-only (auditor) collaborators are blocked at the"
              + " service layer.")
  public CaCollaborationDtos.NoteResponse addNote(
      @Valid @RequestBody CaCollaborationDtos.NoteRequest body) {
    return CaCollaborationDtos.toNote(
        service.addReviewNote(body.entityType(), body.entityId(), body.note()));
  }

  @GetMapping("/notes")
  @Operation(summary = "List review notes for an entity (oldest first)")
  public List<CaCollaborationDtos.NoteResponse> listNotes(
      @RequestParam String entityType, @RequestParam String entityId) {
    return service.listNotes(entityType, entityId).stream()
        .map(CaCollaborationDtos::toNote)
        .toList();
  }

  // -------------------- Approvals (maker-checker) --------------------

  @PostMapping("/approvals")
  @PreAuthorize("hasAnyRole('ACCOUNTANT','CA','OWNER','CO_OWNER')")
  @Operation(summary = "Maker: request approval of a report")
  public CaCollaborationDtos.ApprovalResponse requestApproval(
      @Valid @RequestBody CaCollaborationDtos.ApprovalRequest body) {
    return CaCollaborationDtos.toApproval(
        service.requestApproval(body.reportType(), body.reportRef()));
  }

  @PostMapping("/approvals/{id}/decide")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER')")
  @Operation(
      summary =
          "Checker: approve or reject a requested approval. Guarded by an authority distinct from"
              + " the maker; the domain also enforces approver != requester.")
  public CaCollaborationDtos.ApprovalResponse decideApproval(
      @PathVariable String id, @Valid @RequestBody CaCollaborationDtos.DecideApprovalRequest body) {
    return CaCollaborationDtos.toApproval(service.decideApproval(id, body.approved(), body.note()));
  }

  @GetMapping("/approvals")
  @Operation(summary = "List approvals for a report")
  public List<CaCollaborationDtos.ApprovalResponse> listApprovals(
      @RequestParam String reportType, @RequestParam String reportRef) {
    return service.listApprovals(reportType, reportRef).stream()
        .map(CaCollaborationDtos::toApproval)
        .toList();
  }

  // -------------------- Month-end close checklist --------------------

  @PostMapping("/checklists")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Create the month-end close checklist for a period")
  public CaCollaborationDtos.ChecklistResponse createChecklist(
      @Valid @RequestBody CaCollaborationDtos.CreateChecklistRequest body) {
    return CaCollaborationDtos.toChecklist(
        service.createChecklist(
            body.year(), body.month(), CaCollaborationDtos.toItemSpecs(body.items())));
  }

  @PostMapping("/checklists/items/{itemId}")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER','ACCOUNTANT','CA')")
  @Operation(summary = "Mark a checklist item done/undone")
  public CaCollaborationDtos.ChecklistResponse setChecklistItem(
      @PathVariable String itemId,
      @Valid @RequestBody CaCollaborationDtos.SetChecklistItemRequest body) {
    return CaCollaborationDtos.toChecklist(service.setChecklistItem(itemId, body.done()));
  }

  @GetMapping("/checklists/{id}")
  @Operation(summary = "Get a close checklist by id (includes canLockMonth)")
  public CaCollaborationDtos.ChecklistResponse checklistStatus(@PathVariable String id) {
    return CaCollaborationDtos.toChecklist(service.checklistStatus(id));
  }

  @GetMapping("/checklists")
  @Operation(summary = "Get the close checklist for a period (includes canLockMonth)")
  public CaCollaborationDtos.ChecklistResponse checklistForPeriod(
      @RequestParam int year, @RequestParam int month) {
    return CaCollaborationDtos.toChecklist(service.checklistForPeriod(year, month));
  }
}
