# Production-Readiness Assessment (Sprint 18)

Assessment of the platform against the checklist in `engineering-standards.md §4`. Honest status:
✅ implemented **and** tested in-repo · 🟡 implemented, needs live infra to fully verify · ⏭️ deferred
to an ops/deploy task (not runnable in this repo sandbox).

**Test evidence:** the full backend suite is green on JDK 21 — **337 tests, 0 failures, 0 errors, 2
skipped** (the 2 skips are the Docker-gated Testcontainers integration test, which is now correctly
`disabledWithoutDocker` so unit suites stay green locally and it runs in CI). See
`../uat/acceptance-traceability.md` for the non-negotiable → test mapping.

## Security & compliance
| Item | Status | Notes |
|---|---|---|
| TLS 1.3 edge, mTLS internal; WAF | ⏭️ | Terminates at LB/mesh; configured at deploy. |
| Secrets in manager; rotation; none in repo/images | 🟡 | `.gitignore` blocks key material; `KeyRing` rotation primitive tested; live Vault/KMS wiring is deploy-time. |
| Field-level encryption (PAN/bank/IFSC/UPI/GSTIN/mobile/email) | ✅ | `AesGcmCipher`/`EncryptedStringConverter`; `CryptoTest`, `KeyRingTest`. |
| PII masking by role in UI & logs | ✅ (logs) / 🟡 (UI) | `PiiMaskingConverter` tested; UI masking per app checklist. |
| Pen-test; no critical/high; SBOM; image scan | ⏭️ | External sign-off; CI scan stages defined in standards. |
| DPDP consent/retention/deletion/anonymisation/grievance | ✅ | privacy-service DSR flow; `ErasurePlanTest`, `AnonymizerTest`, `PrivacyServiceTest`. |
| Data localisation (India region) | ⏭️ | Terraform region policy at deploy. |
| Compliance reports show status; no "filed" without ack; CA-review gating | ✅ | `ComplianceReportTest.cannotFileWithoutAcknowledgement`, `.approveRequiresReconciledData`; ca-collaboration approvals. |

## Financial integrity
| Item | Status | Notes |
|---|---|---|
| No hard-delete on financial/audit; append-only | ✅ | evidence trigger + no delete path (`AuditEvidenceServiceTest`); reversal-only ledger. |
| Idempotency on payment/webhook/payout/ledger | ✅ | signature/double-delivery + consumer dedupe tests. |
| Maker-checker on high-risk actions | ✅ | `ReportApprovalTest`, payout approval tests. |
| Reconciliation before final compliance export | ✅ | `approveRequiresReconciledData`; `displayState=UNRECONCILED`. |
| Immutable audit + evidence pack export | ✅ | audit-evidence service + `ExportJob`. |

## Reliability & ops
| Item | Status | Notes |
|---|---|---|
| RDS Multi-AZ; encrypted backups; **PITR tested**; DR runbook | ⏭️ | Rehearsed in Sprint 19; procedure in incident/deploy runbooks. |
| Kafka HA; outbox relay monitored; DLQ + replay | 🟡 | Outbox+idempotent consumers implemented/tested; DLQ/replay + monitoring at deploy. |
| Redis HA; graceful degradation (Redis/Kafka/S3/PG down) | 🟡 | AI assistant degrades gracefully (tested); broader chaos drills need infra. |
| Autoscaling (HPA) + P95 SLOs + alerting | 🟡 | SLOs + scaling guide documented (Sprint 17); HPA/KEDA manifests at deploy. |
| Blue/green + automated rollback rehearsed | 🟡 | Procedure in `deployment-and-rollback.md`; additive migrations guarantee traffic-only rollback. |
| Observability (traces/metrics/logs, dashboards, alerts) | 🟡 | Structured JSON logs + actuator/prometheus in every service; OTel/Loki wiring at deploy. |
| Env separation; prod change via CI/CD only; signed checklists | 🟡 | Trunk-based, protected `main`; pipelines defined in standards. |
| Runbooks (incident, on-call, failover, key rotation) | ✅ | `../security/incident-runbook.md`, `deployment-and-rollback.md`, `../security/secrets-and-key-management.md`. |

## Product
| Item | Status | Notes |
|---|---|---|
| All baseline features present (no feature removed) | ✅ | Sprints 0–17 delivered; see sprint-plan.md DELIVERED notes. |
| UAT sign-off (MSME + accountant + CA) | 🟡 | Scripts ready (`../uat/uat-scripts.md`); human sign-off is the live UAT step. |
| App-store readiness (Android priority) | 🟡 | Checklist in `app-store-readiness.md`; signed builds/console are deploy tasks. |
| Staging smoke + regression green; no critical bugs | ✅ (regression) / 🟡 (staging smoke) | Full suite green; `smoke/smoke.sh` runs against a deployed env. |

## Go / No-go summary

- **Green in-repo:** all business/financial invariants, security governance, DPDP flow, and the full
  337-test regression suite. One real issue found and fixed this sprint (integration test now skips
  cleanly without Docker).
- **Gated on environment (🟡/⏭️):** live infra items — TLS/WAF, KMS/Vault, HA/DR/PITR, HPA/KEDA,
  observability wiring, pen-test sign-off, load-test execution, store submission, and human UAT
  sign-off. These are the Sprint 18→19 deploy activities; procedures and artifacts (runbooks, SLOs,
  k6 scripts, smoke script) are in place.
- **Recommendation:** ready to proceed to staging deployment and the Sprint 19 launch activities;
  production go-live is contingent on completing the 🟡/⏭️ items and capturing sign-offs.
