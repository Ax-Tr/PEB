package com.paywithease.cacollaboration.api;

import com.paywithease.cacollaboration.application.CaCollaborationService;
import com.paywithease.cacollaboration.domain.CaInvite;
import com.paywithease.cacollaboration.domain.CloseChecklistItem;
import com.paywithease.cacollaboration.domain.ReportApproval;
import com.paywithease.cacollaboration.domain.ReviewNote;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Request/response DTOs for the /collaboration API. */
public final class CaCollaborationDtos {

  private CaCollaborationDtos() {}

  // -------------------- Invitations --------------------

  /**
   * Invite an external collaborator. {@code role} must be a valid {@link
   * com.paywithease.cacollaboration.domain.CollaboratorRole}.
   */
  public record InviteRequest(@NotBlank String email, @NotBlank String role) {}

  public record AcceptInviteRequest() {}

  /**
   * Invite view returned to the business owner. Email is returned in the owner's own response
   * (their invite); it is never written to logs — logs mask PII via the logback converter.
   */
  public record InviteResponse(
      String id, String email, String role, String status, String linkedUserId) {}

  // -------------------- Review notes --------------------

  public record NoteRequest(
      @NotBlank String entityType, @NotBlank String entityId, @NotBlank String note) {}

  public record NoteResponse(
      String id, String entityType, String entityId, String authorId, String note) {}

  // -------------------- Approvals (maker-checker) --------------------

  public record ApprovalRequest(@NotBlank String reportType, @NotBlank String reportRef) {}

  public record DecideApprovalRequest(@NotNull Boolean approved, String note) {}

  public record ApprovalResponse(
      String id,
      String reportType,
      String reportRef,
      String status,
      String requestedBy,
      String decidedBy) {}

  // -------------------- Close checklist --------------------

  public record ChecklistItemSpecRequest(@NotBlank String label, boolean mandatory) {}

  public record CreateChecklistRequest(
      @Min(2000) @Max(2100) int year,
      @Min(1) @Max(12) int month,
      @NotEmpty @Valid List<ChecklistItemSpecRequest> items) {}

  public record SetChecklistItemRequest(@NotNull Boolean done) {}

  public record ChecklistItemResponse(
      String id, String label, boolean mandatory, boolean done, int sortOrder) {}

  public record ChecklistResponse(
      String id, int year, int month, boolean canLockMonth, List<ChecklistItemResponse> items) {}

  // -------------------- Mappers --------------------

  static InviteResponse toInvite(CaInvite i) {
    return new InviteResponse(
        i.getId(), i.getEmail(), i.getRole(), i.getStatus(), i.getLinkedUserId());
  }

  static NoteResponse toNote(ReviewNote n) {
    return new NoteResponse(
        n.getId(), n.getEntityType(), n.getEntityId(), n.getAuthorId(), n.getNote());
  }

  static ApprovalResponse toApproval(ReportApproval a) {
    return new ApprovalResponse(
        a.getId(),
        a.getReportType(),
        a.getReportRef(),
        a.getStatus(),
        a.getRequestedBy(),
        a.getDecidedBy());
  }

  static ChecklistResponse toChecklist(CaCollaborationService.ChecklistView view) {
    List<ChecklistItemResponse> items =
        view.items().stream().map(CaCollaborationDtos::toChecklistItem).toList();
    return new ChecklistResponse(
        view.checklist().getId(),
        view.checklist().getPeriodYear(),
        view.checklist().getPeriodMonth(),
        view.canLockMonth(),
        items);
  }

  static ChecklistItemResponse toChecklistItem(CloseChecklistItem item) {
    return new ChecklistItemResponse(
        item.getId(), item.getLabel(), item.isMandatory(), item.isDone(), item.getSortOrder());
  }

  static List<CaCollaborationService.ItemSpec> toItemSpecs(List<ChecklistItemSpecRequest> items) {
    return items.stream()
        .map(r -> new CaCollaborationService.ItemSpec(r.label(), r.mandatory()))
        .toList();
  }

  static void requireValidRole(String role) {
    if (role == null || !com.paywithease.cacollaboration.domain.CollaboratorRole.isValid(role)) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown collaborator role: " + role);
    }
  }
}
