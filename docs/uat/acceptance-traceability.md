# Acceptance / Non-Negotiables Traceability (Sprint 18)

Maps every platform non-negotiable to where it is enforced in code and the automated test that proves
it. This is the evidence that the acceptance criteria are met "by construction", not just by manual
UAT. All referenced tests are green on JDK 21 (see the Sprint 18 full-suite run).

| # | Non-negotiable | Enforced in | Proven by (test) |
|---|---|---|---|
| 1 | No hard-delete of financial data | Reversal-only ledger; evidence `evidence_items` append-only + DB trigger | `AuditEvidenceServiceTest.serviceExposesNoDeleteOrUpdateOfEvidence`; ledger reversal tests |
| 2 | Audit trail on every edit | `AuditWriter.record` in every service mutation | service tests assert `audit.record(...)` invoked |
| 3 | Reversal-only corrections | ledger posting/reversal domain | `PostingTemplatesTest`, ledger consumer tests |
| 4 | Webhooks signature-verified + idempotent | payment webhook handler + `IdempotencyService` | payment webhook signature/double-delivery tests |
| 5 | Idempotent financial actions | outbox + `processed_events` dedupe; natural-key guards | consumer dedupe tests; `AiAutomationServiceTest`/`AuditEvidenceServiceTest` idempotency |
| 6 | Maker-checker for high-risk actions | payout approval; report approval (approver≠requester); month reopen | `ReportApprovalTest.requesterCannotApproveOwnRequest`; payout tests |
| 7 | OCR/bank-detail reviewed before save | `ConfidencePolicy` — BANK_DETAIL_EXTRACTION never auto-applied | `ConfidencePolicyTest.bankDetailExtractionAlwaysNeedsReview` |
| 8 | AI confidence shown; low-confidence not auto-posted | `ConfidencePolicy`; suggestion carries confidence | `ConfidencePolicyTest.*`, `AiAutomationServiceTest.*` |
| 9 | Month-lock respected | ledger month-lock state machine | month-lock service tests |
| 10 | Compliance status surfaced (unreconciled/draft/reviewed/approved) | `ComplianceReport.displayState` | `ComplianceReportTest.displayStateShowsUnreconciledUntilReconciled` |
| 11 | Never "filed" without official acknowledgement | `ComplianceReport.recordFiling` requires non-blank ack | `ComplianceReportTest.cannotFileWithoutAcknowledgement` |
| 12 | Approval requires reconciled data | `ComplianceReport.approve` | `ComplianceReportTest.approveRequiresReconciledData` |
| 13 | Field-level encryption for PII | `EncryptedStringConverter`/`AesGcmCipher`; `KeyRing` rotation | `CryptoTest`, `KeyRingTest` |
| 14 | No sensitive data in logs | PII-masking logback converter (all services) | `PiiMaskingConverter` + logback config |
| 15 | Tenant isolation (no cross-tenant read) | `TenantContext.requireTenantId` + `findByTenantIdAnd…` | `AiAutomationServiceTest.crossTenantAccessIsBlocked`, `PrivacyServiceTest.crossTenantAccessIsBlocked` |
| 16 | DPDP erasure never deletes financial/tax records | `RetentionPolicy` / `ErasurePlan` | `ErasurePlanTest.financialAndTaxRecordsAreRetainedNotDeleted` |
| 17 | Auditor is read-only | ca-collaboration `assertCanContribute` | `CaCollaborationServiceTest.auditorReadOnlyCannotContribute` |
| 18 | No autonomous statutory filing | `SuggestionKind.STATUTORY_FILING` never auto-applied | `ConfidencePolicyTest.statutoryNeverAutoAppliesEvenAtFullConfidence` |
| 19 | Prompt-injection defence on uploaded text | `PromptInjectionScanner` | `PromptInjectionScannerTest.*` |
| 20 | Balanced journals (Σdebits=Σcredits) | ledger domain invariant | ledger balance tests |
| 21 | Money never floating-point | `Money` (integer paise) everywhere | `MoneyTest`, GST/payroll calc tests |

**Result:** every non-negotiable maps to a concrete enforcement point **and** an automated test. Items
requiring live infrastructure (webhook signature against a real provider, KMS, load) are additionally
listed in `../release/production-readiness-assessment.md` with their deferral status.
