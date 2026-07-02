# Security Incident Runbook (Sprint 16)

Purpose: a concise, actionable playbook for responding to a security incident affecting PEB. Keep it
short enough to use under pressure. Roles: **IC** (incident commander), **Sec** (security lead),
**Ops** (platform/on-call), **DPO** (data-protection officer), **Comms**.

## Severity

| Sev | Definition | Target ack | Target contain |
|---|---|---|---|
| SEV1 | Active data breach, funds at risk, or full outage | 15 min | 1 hour |
| SEV2 | Partial outage, single-tenant exposure, credential leak | 30 min | 4 hours |
| SEV3 | Suspicious activity, low-risk vuln in prod | 1 business day | 5 business days |

## Response phases

1. **Detect & declare.** Trigger sources: SIEM alert, anomaly alert (ai-automation), failed-auth
   spike, provider notice, or report. Declare severity; IC opens an incident channel and timeline.
2. **Contain.**
   - Credential/key leak → rotate the affected secret immediately (JWT signing key, provider key,
     field-encryption key via `KeyRing.withNewActiveKey`, DB creds). Revoke leaked tokens.
   - Account/tenant compromise → disable the actor; revoke sessions; for a compromised collaborator,
     revoke the CA invite (removes access mid-review).
   - Malicious traffic → tighten gateway rate limits / WAF; block source.
3. **Eradicate.** Patch the vulnerability; invalidate exploited artifacts; verify no persistence
   (check audit trail — it is immutable and cannot be erased by the attacker).
4. **Recover.** Restore service; re-enable accounts after reset; run the `KeyRing` re-encryption
   sweep if a field key was rotated; confirm health and reconciliations.
5. **Post-incident.** Blameless review within 5 business days; update this runbook and the threat
   model; add a regression test for the root cause.

## Evidence & forensics

- The **immutable audit trail** (`audit_events`, per service) and the **evidence room**
  (audit-evidence-service, SHA-256, DB-trigger-protected) are the primary evidence sources — they
  cannot be tampered with or deleted, including by an attacker with DB access.
- Preserve logs (JSON, PII-masked) and Kafka offsets; snapshot affected DBs before remediation.

## Regulatory / DPDP

- If personal data is breached, **DPO** assesses notifiability under the DPDP Act and informs the
  Data Protection Board and affected data principals within the required timeline.
- Log the incident and actions; a data principal may raise a **grievance** via privacy-service
  (`GRIEVANCE` DSR), which is tracked to SLA.

## Key contacts (fill per environment)

- On-call / PagerDuty rotation: _____
- KMS / secret-manager console: _____
- Payment/notification provider security contacts: _____
- Cloud provider support (SEV1): _____
