package com.paywithease.cacollaboration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.cacollaboration.domain.CaInvite;
import com.paywithease.cacollaboration.domain.CloseChecklistItem;
import com.paywithease.cacollaboration.domain.CollaboratorRole;
import com.paywithease.cacollaboration.domain.ReportApproval;
import com.paywithease.cacollaboration.infrastructure.CaInviteRepository;
import com.paywithease.cacollaboration.infrastructure.CloseChecklistItemRepository;
import com.paywithease.cacollaboration.infrastructure.CloseChecklistRepository;
import com.paywithease.cacollaboration.infrastructure.ReportApprovalRepository;
import com.paywithease.cacollaboration.infrastructure.ReviewNoteRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaCollaborationServiceTest {

  @Mock CaInviteRepository invites;
  @Mock ReviewNoteRepository notes;
  @Mock ReportApprovalRepository approvals;
  @Mock CloseChecklistRepository checklists;
  @Mock CloseChecklistItemRepository checklistItems;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private CaCollaborationService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new CaCollaborationService(
            invites,
            notes,
            approvals,
            checklists,
            checklistItems,
            audit,
            outbox,
            objectMapper,
            clock,
            14);
    setActor("owner1");
    when(invites.save(any())).thenAnswer(returnsFirstArg());
    when(notes.save(any())).thenAnswer(returnsFirstArg());
    when(approvals.save(any())).thenAnswer(returnsFirstArg());
    when(checklists.save(any())).thenAnswer(returnsFirstArg());
    when(checklistItems.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private void setActor(String actorId) {
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", actorId, "corr1"));
  }

  @Test
  void inviteCreatesPendingInvitation() {
    CaInvite invite = service.invite("ca@example.com", CollaboratorRole.CA);
    assertThat(invite.getStatus()).isEqualTo("PENDING");
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void internalUserCanAddReviewNote() {
    when(invites.findByTenantIdAndLinkedUserId("tenant1", "owner1")).thenReturn(List.of());
    service.addReviewNote("COMPLIANCE_REPORT", "rep1", "Please recheck ITC");
    verify(notes).save(any());
  }

  @Test
  void revokedCollaboratorCannotContributeMidReview() {
    CaInvite revoked =
        new CaInvite(
            "i1",
            "tenant1",
            "ca@example.com",
            CollaboratorRole.CA,
            "owner1",
            clock.instant().plusSeconds(3600),
            clock.instant());
    revoked.accept("caUser", clock.instant());
    revoked.revoke(clock.instant());
    setActor("caUser");
    when(invites.findByTenantIdAndLinkedUserId("tenant1", "caUser")).thenReturn(List.of(revoked));

    assertThatThrownBy(() -> service.addReviewNote("COMPLIANCE_REPORT", "rep1", "note"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("access is not active");
  }

  @Test
  void auditorReadOnlyCannotContribute() {
    CaInvite auditor =
        new CaInvite(
            "i2",
            "tenant1",
            "aud@example.com",
            CollaboratorRole.AUDITOR,
            "owner1",
            clock.instant().plusSeconds(3600),
            clock.instant());
    auditor.accept("audUser", clock.instant());
    setActor("audUser");
    when(invites.findByTenantIdAndLinkedUserId("tenant1", "audUser")).thenReturn(List.of(auditor));

    assertThatThrownBy(() -> service.addReviewNote("COMPLIANCE_REPORT", "rep1", "note"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("read-only");
  }

  @Test
  void requestApprovalEmitsAndBlocksDuplicatePending() {
    when(invites.findByTenantIdAndLinkedUserId(any(), any())).thenReturn(List.of());
    when(approvals.existsByTenantIdAndReportTypeAndReportRefAndStatus(
            "tenant1", "GSTR3B_SUMMARY", "rep1", "REQUESTED"))
        .thenReturn(false);

    service.requestApproval("GSTR3B_SUMMARY", "rep1");
    verify(outbox).append(any());

    when(approvals.existsByTenantIdAndReportTypeAndReportRefAndStatus(
            "tenant1", "GSTR3B_SUMMARY", "rep1", "REQUESTED"))
        .thenReturn(true);
    assertThatThrownBy(() -> service.requestApproval("GSTR3B_SUMMARY", "rep1"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already pending");
  }

  @Test
  void decideApprovalEnforcesMakerChecker() {
    when(invites.findByTenantIdAndLinkedUserId(any(), any())).thenReturn(List.of());
    ReportApproval approval =
        new ReportApproval("a1", "tenant1", "GSTR3B_SUMMARY", "rep1", "maker1", clock.instant());
    when(approvals.findByTenantIdAndId("tenant1", "a1")).thenReturn(Optional.of(approval));

    setActor("maker1");
    assertThatThrownBy(() -> service.decideApproval("a1", true, null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Maker-checker");

    setActor("checker1");
    ReportApproval decided = service.decideApproval("a1", true, "ok");
    assertThat(decided.getStatus()).isEqualTo("APPROVED");
    verify(outbox, org.mockito.Mockito.atLeastOnce()).append(any());
  }

  @Test
  void checklistItemToggleDrivesCanLockMonth() {
    var checklist =
        new com.paywithease.cacollaboration.domain.CloseChecklist(
            "c1", "tenant1", 2026, 5, "owner1", clock.instant());
    CloseChecklistItem item =
        new CloseChecklistItem("it1", "c1", "tenant1", "Reconcile bank", true, 0);
    when(checklistItems.findByTenantIdAndId("tenant1", "it1")).thenReturn(Optional.of(item));
    when(checklists.findByTenantIdAndId("tenant1", "c1")).thenReturn(Optional.of(checklist));
    when(checklistItems.findByChecklistIdOrderBySortOrderAsc("c1")).thenReturn(List.of(item));

    var view = service.setChecklistItem("it1", true);
    assertThat(view.canLockMonth()).isTrue();
  }
}
