# PDF Gap Closure Sprint Plan

This plan records the features required to close the gaps found while comparing the uploaded
PayWithEase PDF against the developed application. These are product differentiators: commitment
tracking, visible installment/reminder workflows, real OCR bank capture, and voice financial input.

Delivery model: 2-week sprints. All work follows the existing fintech non-negotiables: tenant
isolation, idempotency, audit trail on every state change, no hard-delete of financial records,
integer-paise money, PII encryption, no sensitive logs, and human review before OCR/AI/voice output
is applied.

## Sprint 20 - Commitment Tracking Engine

**Goal:** Let owners record, monitor, reschedule, and close customer/vendor payment promises with
due dates and broken-promise tracking.

**Scope**
- New `commitment-service`.
- Commitment creation from manual entry, invoice, payment request, installment, and future voice
  drafts.
- Status lifecycle: `PROMISED`, `PARTIALLY_PAID`, `PAID`, `BROKEN`, `RESCHEDULED`, `CANCELLED`.
- Due-soon and overdue detection.
- Reminder scheduling hook.
- Mobile screens for list, create, detail, reschedule, and mark-paid.
- Dashboard cards for due today, due soon, overdue, and broken promises.

**Backend Checklist**
- [x] Add `backend/commitment-service` to Gradle settings and gateway routes.
- [x] Add Flyway migrations for `commitments` and `commitment_events`.
- [x] Implement domain state machine and validation rules.
- [x] Implement REST APIs:
  - [x] `POST /api/v1/commitments`
  - [x] `GET /api/v1/commitments`
  - [x] `GET /api/v1/commitments/{id}`
  - [x] `POST /api/v1/commitments/{id}/record-payment`
  - [x] `POST /api/v1/commitments/{id}/reschedule`
  - [x] `POST /api/v1/commitments/{id}/cancel`
  - [x] `GET /api/v1/commitments/due-soon`
  - [x] `GET /api/v1/commitments/overdue`
- [x] Emit events: `COMMITMENT_CREATED`, `COMMITMENT_PARTIALLY_PAID`, `COMMITMENT_PAID`,
  `COMMITMENT_RESCHEDULED`, `COMMITMENT_BROKEN`, `COMMITMENT_CANCELLED`.
- [ ] Add payment/invoice/installment event consumers where source events can update commitments.
- [x] Record immutable audit events for every transition.
- [x] Add tenant-scoped repositories and cross-tenant access tests.

**Frontend Checklist**
- [x] Add `src/features/commitments`.
- [x] Add navigation entry under Books or a main tab if product wants it first-class.
- [x] Build `CommitmentListScreen`.
- [x] Build `CommitmentCreateScreen`.
- [x] Build `CommitmentDetailScreen` with timeline.
- [x] Build reschedule and cancel flows.
- [ ] Add "Create commitment" from Receive and Invoice flows.
- [x] Add Home dashboard commitment cards.
- [ ] Add empty/error/loading states and offline queue support for create/reschedule.

**Acceptance Criteria**
- [x] Owner can create a commitment with counterparty, amount, due date, and source.
- [x] Partial payment updates outstanding amount and status.
- [x] Overdue commitment becomes visible as overdue without manual tagging.
- [x] Rescheduling preserves original promise history.
- [x] Every state change is auditable.
- [x] Duplicate create/mark-paid submissions are idempotent.

**Tests**
- [x] Domain lifecycle tests for every valid and invalid transition.
- [x] API tests for tenant isolation and validation.
- [ ] Event consumer idempotency tests.
- [ ] Frontend tests for create/list/detail states.
- [ ] E2E happy path: create commitment, send reminder, record partial payment, reschedule, close.

## Sprint 21 - Installment And Reminder Mobile UX

**Goal:** Expose the existing installment and reminder capabilities as simple owner-facing workflows.

**Scope**
- Mobile screens for receivable/payable installments.
- Mobile screens for reminders and templates.
- Commitment-to-reminder automation.
- Installment-to-reminder automation.
- Due-date status badges and quick actions.

**Backend Checklist**
- [x] Review existing installment APIs for mobile completeness.
- [ ] Add remaining installment list filters: status, due date, counterparty.
- [ ] Add reminder rules for D-3, D-1, due date, D+2, and escalation.
- [x] Add commitment and installment reminder source references.
- [x] Add delivery-status visibility for reminder logs.
- [ ] Add events or consumers for `COMMITMENT_CREATED` and `INSTALLMENT_SCHEDULE_CREATED`.

**Frontend Checklist**
- [x] Add `InstallmentListScreen`.
- [x] Add `InstallmentCreateScreen`.
- [x] Add `InstallmentDetailScreen` with EMI timeline.
- [ ] Add `InstallmentPaymentScreen` or a dedicated payment modal.
- [x] Add `ReminderListScreen`.
- [x] Add `ReminderCreateScreen`.
- [ ] Add reminder preview and send-now action.
- [ ] Link installments to invoice, commitment, and vendor/payable flows.
- [x] Add reminders and installments to Books menu.

**Acceptance Criteria**
- [x] Owner can create a receivable or payable installment schedule.
- [ ] App shows paid, due-soon, overdue, and upcoming EMIs.
- [x] Owner can record EMI payment and see balance auto-update.
- [x] Owner can schedule reminders from commitment/installment/invoice context.
- [x] Reminder delivery status is visible.
- [x] WhatsApp/SMS/email providers remain adapter-based and contract-gated.

**Tests**
- [x] EMI schedule generation and rounding tests.
- [ ] Reminder scheduling tests across D-3/D-1/D-day/D+2.
- [ ] Frontend list/detail/create tests.
- [ ] E2E path: invoice partial payment -> installment schedule -> reminder -> EMI paid.

## Sprint 22 - Real OCR Bank Capture

**Goal:** Implement actual screenshot/photo-to-bank-details extraction with mandatory human review.

**Scope**
- New `ocr-document-service`.
- Document upload flow with signed URLs.
- OCR provider adapter interface.
- Bank-detail field extraction: account holder, account number, IFSC, bank name, UPI ID.
- Confidence per field.
- Review screen before vendor bank details become usable.
- Integration with vendor bank account creation and AI suggestion governance.

**Backend Checklist**
- [x] Add `backend/ocr-document-service` to Gradle settings and gateway routes.
- [x] Add Flyway migrations for `documents` and `ocr_jobs`.
- [x] Implement signed-upload URL API.
- [x] Implement OCR provider port and development/mock adapter.
- [x] Add production-ready adapter boundary for Google Document AI, AWS Textract, or Azure Document
  Intelligence.
- [x] Validate IFSC, account-number shape, UPI ID, and field confidence.
- [ ] Integrate OCR result with `ai-automation-service` bank-detail suggestion.
- [x] Integrate reviewed result with `vendor-service` bank account creation as `PENDING_REVIEW`.
- [x] Add file-type/size validation.
- [ ] Add malware scanning before production object storage accepts uploads.
- [x] Ensure OCR raw text is encrypted or protected as sensitive data and never logged.

**Frontend Checklist**
- [ ] Add camera/gallery upload for vendor bank details.
- [x] Add OCR job progress state.
- [x] Add extracted-fields review screen.
- [x] Show confidence per field.
- [ ] Allow editing before confirmation.
- [x] Save only after explicit user confirmation.
- [ ] Add fallback manual entry path.

**Acceptance Criteria**
- [x] User can reserve a bank-details upload and receive extracted fields from OCR text capture.
- [ ] Low-confidence fields are visually highlighted.
- [x] OCR output is never auto-applied.
- [x] Confirmed bank detail remains gated by vendor review policy.
- [x] Sensitive OCR text and account details do not appear in logs.

**Tests**
- [x] OCR parser tests for common bank-detail formats.
- [x] IFSC/UPI/account validation tests.
- [x] Security tests for invalid file type and oversized upload.
- [x] Review-gate tests proving OCR bank details cannot be used without confirmation.
- [ ] Frontend review/edit/confirm tests.

## Sprint 23 - Voice Financial Input

**Goal:** Let owners speak natural financial instructions and convert them into reviewed drafts.

**Scope**
- Mobile voice capture.
- Speech-to-text adapter boundary.
- AI intent parsing in `ai-automation-service`.
- Draft review workflow.
- Supported intents: create commitment, create installment, create reminder, create expense, create
  payout reminder, add customer note.
- Human approval required before any record is created.

**Backend Checklist**
- [x] Add `voice_drafts` migration in `ai-automation-service` or a dedicated voice module.
- [x] Implement `POST /api/v1/ai/voice/parse`.
- [x] Implement `GET /api/v1/ai/voice/drafts`.
- [x] Implement `POST /api/v1/ai/voice/drafts/{id}/approve`.
- [x] Implement `POST /api/v1/ai/voice/drafts/{id}/reject`.
- [x] Add intent parser port and deterministic fallback rules.
- [x] Add prompt-injection scanning for transcripts.
- [x] Add confidence scoring and missing-field detection.
- [x] On approval, call the correct domain service using idempotency keys.
- [x] Store transcript as sensitive business data.

**Frontend Checklist**
- [x] Add reusable `VoiceCaptureModal`.
- [ ] Add microphone entry point on Home, Receive, Commitments, and Pay.
- [x] Add `VoiceDraftReviewScreen`.
- [x] Show transcript, detected action, confidence, extracted fields, and missing fields.
- [x] Allow edit before approval.
- [x] Allow discard/reject with reason.
- [ ] Add offline-safe draft creation where possible.

**Acceptance Criteria**
- [x] User can say "Raj promised to pay 5000 on Friday" and get a commitment draft.
- [x] User can review and edit extracted fields before creation.
- [x] Low-confidence or incomplete drafts require edits before approval.
- [x] Voice input never posts payment, payout, invoice, or ledger records directly.
- [x] Suspicious instructions are neutralized and flagged.

**Tests**
- [x] Intent parsing tests for dates, amounts, and counterparties.
- [ ] Mixed-language phrase parsing tests.
- [x] Prompt-injection tests for transcript text.
- [x] Draft approval idempotency tests.
- [ ] Frontend modal and review-flow tests.
- [ ] E2E path: voice -> draft commitment -> approve -> commitment created -> reminder scheduled.

## Sprint 24 - Gap Closure Analytics And Hardening

**Goal:** Turn the new operational workflows into actionable owner intelligence and harden them for
production rollout.

**Scope**
- Commitment analytics.
- Broken-promise analytics.
- Collection efficiency.
- Upcoming obligations dashboard.
- Product-level profitability event enrichment where possible.
- UAT scripts for all PDF-gap workflows.

**Backend Checklist**
- [x] Add analytics endpoints:
  - [x] `GET /api/v1/analytics/commitments-summary`
  - [x] `GET /api/v1/analytics/collection-efficiency`
  - [x] `GET /api/v1/analytics/broken-promises`
  - [x] `GET /api/v1/analytics/upcoming-obligations`
- [x] Consume commitment events into analytics read models.
- [ ] Consume installment/reminder events into analytics read models.
- [ ] Add event enrichment for invoice line/product profitability where feasible.
- [x] Add freshness indicators for commitment streams.
- [ ] Add freshness indicators for reminder streams.
- [ ] Add SLOs for new endpoints.

**Frontend Checklist**
- [x] Add dashboard cards for due today, overdue commitments, broken promises, and upcoming obligations.
- [x] Add insight cards for collection efficiency and broken promises.
- [ ] Add drill-down from dashboard cards to filtered commitment/installment lists.
- [ ] Add feature flags for staged rollout.

**Acceptance Criteria**
- [x] Owner can answer: who promised to pay, who missed, how much is due today, and what is coming due.
- [x] Analytics read models show freshness/staleness.
- [x] Dashboard does not hit OLTP tables.
- [x] UAT scripts cover commitment, installment, reminder, OCR, and voice workflows.

**Tests**
- [x] Read-model ingestion tests.
- [x] Analytics calculation tests.
- [ ] Dashboard frontend tests.
- [ ] UAT checklist completed with no critical bugs.

## Cross-Sprint Release Checklist

- [ ] Architecture diagram updated for commitment and OCR services.
- [ ] API docs/OpenAPI generated for all new endpoints.
- [ ] Gateway routes added and smoke-tested.
- [ ] RBAC matrix updated for owner, cashier, accountant, CA, and auditor.
- [ ] Tenant isolation tests added for all new services.
- [ ] Idempotency tests added for all financial or state-changing endpoints.
- [ ] Audit events emitted and verified for every state transition.
- [ ] PII/sensitive data encryption and log masking verified.
- [ ] Offline queue behavior defined for mobile create/update actions.
- [ ] Push/SMS/email/WhatsApp provider behavior documented and contract-gated.
- [ ] Performance SLOs added for new list/dashboard endpoints.
- [ ] UAT scripts updated for the PDF gap workflows.
- [ ] Production-readiness assessment updated before rollout.
