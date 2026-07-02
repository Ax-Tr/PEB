# ai-automation-service

Governance-first AI automation for MSME finance. Every AI output is **advisory, scored, and
auditable** — the service implements the governance *around* AI, never autonomous action.

## What it does

- **Transaction classification** — classifies a bank-transaction narration into a category as a
  governed suggestion.
- **OCR bank-detail review** — records an OCR bank-detail extraction as a suggestion that is
  **never auto-applied**; the user must review before the detail is saved.
- **Anomaly detection** — flags an observed metric value that deviates from its history and raises
  an alert (a human acknowledges or dismisses it).
- **Cashflow forecast** — an advisory next-period forecast, stored as a (never auto-applied)
  suggestion.
- **NL assistant** — answers finance questions from tenant-scoped context only; it is advisory and
  can perform no statutory or financial action.

## Governance model

- **Every AI output shows its confidence/score.** Suggestion responses carry `confidence`; anomaly
  results carry `score`.
- **Low-confidence and statutory outputs are never auto-applied.** A confidence policy decides
  auto-apply vs. review; only auto-applicable, non-statutory kinds may ever auto-apply. Humans
  **accept/reject** suggestions and **acknowledge/dismiss** alerts — these governance decisions are
  guarded by a higher-privilege authority than merely producing a suggestion.
- **No autonomous statutory filing.** There is deliberately no filing action in this service.
- **Prompt-injection scanning.** Assistant inputs are scanned and neutralised before they reach any
  model; a blocked attempt is audited.
- **Graceful degradation.** When no model is available the assistant returns a manual-review answer
  with `modelAvailable=false`.
- **Tenant-scoped & auditable.** All reads/writes are scoped by `TenantContext`; no endpoint accepts
  a `tenantId`, so no cross-tenant access is possible. All decisions are audited.

## API

Base path `/api/v1/ai`. All endpoints require a valid JWT (resource server). Port **8100**,
database `ai_automation_db`.

Suggestions:

- `POST /suggestions/classify-transaction` `{subjectId, narration}` — classify a transaction *(OWNER/CO_OWNER/ACCOUNTANT/CA/CASHIER)*
- `POST /suggestions/bank-detail` `{subjectId, fields:{k:v}, confidence}` — OCR bank-detail review *(OWNER/CO_OWNER/ACCOUNTANT/CA/CASHIER)*
- `POST /suggestions/cashflow-forecast` `{periodNets:[...]}` — advisory forecast *(OWNER/CO_OWNER/ACCOUNTANT/CA/CASHIER)*
- `GET  /suggestions?status=` — list suggestions (status optional) *(authenticated)*
- `GET  /suggestions/{id}` — get a suggestion *(authenticated)*
- `POST /suggestions/{id}/accept` — human approval *(OWNER/CO_OWNER/ACCOUNTANT/CA)*
- `POST /suggestions/{id}/reject` — human rejection *(OWNER/CO_OWNER/ACCOUNTANT/CA)*
- `POST /suggestions/{id}/feedback` `{helpful, note}` — feedback on a suggestion *(authenticated)*

Anomalies:

- `POST /anomalies/detect` `{subjectType, subjectId, metric, history:[...], observed}` — evaluate *(OWNER/CO_OWNER/ACCOUNTANT/CA/CASHIER)*
- `GET  /anomalies?status=` — list alerts (status optional) *(authenticated)*
- `POST /anomalies/{id}/acknowledge` — acknowledge an alert *(OWNER/CO_OWNER/ACCOUNTANT/CA)*
- `POST /anomalies/{id}/dismiss` — dismiss an alert *(OWNER/CO_OWNER/ACCOUNTANT/CA)*

Assistant:

- `POST /assistant/ask` `{question}` — ask the advisory assistant *(authenticated)*

## Events

Subscribes to `ingestion.events`. Emits `AI_SUGGESTION_CREATED` and `ANOMALY_DETECTED` via the
transactional outbox.

## Notes

- **The OCR extraction engine (image → text) and a live LLM are external adapters that are not wired
  in this environment.** This service implements the governance *around* them: it stores OCR
  extractions as review-required suggestions, and the assistant degrades to a manual-review response
  (`modelAvailable=false`) because the `AiAssistantPort` falls back to `UnavailableAssistant`.
- **Auto-classification from `ingestion.events` is deferred.** transaction-ingestion-service emits
  `TRANSACTION_CLASSIFIED` / `BANK_TRANSACTION_IMPORTED`, but the event payload carries only
  `transactionId, source, direction, amountMinor, category, classificationStatus` — **not** the
  narration text. A meaningful classification is impossible without the narration, so the consumer is
  a documented no-op until the upstream event schema is enriched to carry it (see
  `AiEventConsumer`).
