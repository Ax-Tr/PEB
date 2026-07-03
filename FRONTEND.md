# PEB Frontend (Expo + React Native Web + TypeScript)

One TypeScript codebase that runs on **Android, iOS, and the web browser** (via `react-native-web`) —
"any device" from a single source. It talks to the PEB backend through the API gateway (`/api/v1/**`);
no mock data.

## Run

```bash
npm install
npm run web        # open in a browser (any device via responsive layout)
npm run android    # Android emulator/device (Metro)
npm run ios        # iOS simulator (macOS)

npm run e2e:build  # Detox: build the app for an emulator (needs device tooling)
npm run e2e:test   # Detox: run e2e specs (e2e/*.e2e.ts)
```

Point it at the backend with an env var (defaults: web → same origin; Android emulator →
`http://10.0.2.2:8080`; iOS/other → `http://localhost:8080`):

```bash
EXPO_PUBLIC_API_BASE_URL=https://staging.example npm run web
```

## Verify (CI gate — the frontend analog of the backend's compile+test)

```bash
npm run typecheck   # tsc --noEmit — strict, zero errors
npm test            # jest — pure data-layer unit tests
```

Current status: **`tsc --noEmit` clean; 23 unit tests green** (money, HTTP client, auth flow, offline
queue, i18n). Detox e2e specs (`e2e/`) run on an emulator/simulator in CI — not in this sandbox.

## Structure

```
App.tsx                     # app root: providers (Query, Auth, SafeArea, Navigation)
src/
  shared/                   # framework-free data layer (unit-tested)
    money.ts                #   integer-paise money + INR formatting (never float)
    http.ts                 #   typed client: bearer token, correlation id, idempotency key,
                            #   RFC-7807 error parsing, silent refresh on 401
    auth.ts / tokens.ts     #   phone+OTP, token storage contract, refresh (TokenProvider)
    tokenStore.ts           #   platform impl: SecureStore (native) / localStorage (web)
    types.ts                #   API response types (money fields are paise)
    api.ts                  #   composition root (auth + http instances)
    queryClient.ts          #   TanStack Query config (no retry on 4xx)
    config.ts / constants.ts
  components/                # Screen, Button, TextField, Card, Money, QueryState (4-state)
  features/
    auth/                    #   AuthContext + LoginScreen (phone → OTP)
    dashboard/               #   Home: P&L / cashflow / receivables + freshness badge (analytics API)
    receive/                 #   Collect money: amount → UPI payment request + QR (payment API)
    pay/                     #   Pay vendor: create payout + risk badge + maker-checker approve/reject
    invoices/                #   Create GST invoice (server-computed tax) + send
    reconciliation/          #   Run engine, review suggested matches, confirm/reject
    compliance/              #   Generate/list reports + detail: reconcile→review→approve→file (ack)
    books/                   #   Books menu (invoices / reconciliation / compliance)
    dashboard/InsightsScreen #   Insights: receivables/payables aging, cashflow by month, profitability
    privacy/                 #   DPDP data rights: submit + list + lifecycle (verify→plan→complete)
    ai/                      #   AI assistant (ask) + suggestions (accept/reject) + anomaly triage
    more/                    #   More menu (insights / assistant / data rights)
    masters/                 #   customer + vendor/beneficiary hooks (pickers, not raw IDs)
    offline/                 #   OfflineQueueProvider (post-or-queue, flush on reconnect)
    i18n/                    #   I18nProvider + useI18n (en/hi, switchable in More)
  navigation/                # RootNavigator (auth gate → tabs: Home/Receive/Pay/Books/More) + Books/More stacks
  theme/                     # design tokens (shared native + web)
  shared/
    i18n.ts                  #   pure translate() + en/hi dictionaries (unit-tested)
    stepUp.ts                #   biometric step-up (expo-local-authentication) → X-Step-Up-Verified
  components/BarChart.tsx    # pure react-native-svg signed bar chart (native + web)
e2e/                         # Detox specs (auth.e2e.ts, receive.e2e.ts) + jest config
.detoxrc.js                  # Detox device/app configuration
```

## Charts, i18n, biometric step-up, e2e

- **Charts:** `BarChart` is a dependency-free `react-native-svg` signed bar chart (positive green /
  negative red), used on Insights for cashflow-by-month; renders identically on native and web.
- **i18n:** `shared/i18n.ts` is a pure `translate(locale, key, params)` with **en + hi** dictionaries
  (fallback en → key; `{param}` interpolation), wrapped by `I18nProvider`/`useI18n`. Language is
  switchable under **More**; login and tab labels are localised. Pure engine is unit-tested.
- **Biometric step-up:** high-value payouts (≥ ₹50,000) prompt `expo-local-authentication`
  (device biometric) on native, degrade to an explicit confirm on web, and pass
  `X-Step-Up-Verified: true` to the backend which requires it for high-risk flows.
- **Detox e2e:** `.detoxrc.js` + `e2e/*.e2e.ts` drive login→dashboard and receive-payment against a
  build on an emulator/simulator; specs target stable `testID`s (i18n-proof). Runs in CI with device
  tooling — not in this repo sandbox.

## Offline "Pending Sync"

`shared/offlineQueue.ts` is a framework-free FIFO queue (unit-tested): deferrable mutations are
persisted (`shared/queueStorage.ts` via AsyncStorage) when a POST fails on the network, flushed in
order on reconnect, and each carries an idempotency key so a replay never double-applies. A header
`PendingSyncBadge` shows the count and retries on tap. **Money-moving actions are never queued** —
payment/payout confirmation requires online (per the product rules); only safe, idempotent writes
(e.g. AI feedback) use it.

## Conventions (per engineering-standards §2)

- **Money is integer paise** end-to-end; formatting only at the render boundary (`money.ts`, `Money`).
- **Secure token storage** (Keystore/Keychain on native; localStorage fallback on web) with silent
  refresh; tokens never in plain AsyncStorage.
- Every server-driven view renders **loading / error / empty / success** via `QueryState`.
- Accessibility labels + 48px tap targets on interactive components.
- Typed API layer; errors surface the backend's RFC-7807 `detail`.

## Delivered so far & next

**Done (increments 1–3), type-checked + 18 unit tests green:** shared data layer · auth (phone/OTP) ·
dashboard + insights (analytics) · receive-payment (QR) · pay/payout (risk + maker-checker) · invoice
create (GST) + send · reconciliation (run + confirm/reject) · compliance reports (full lifecycle:
reconcile → review → approve → record filing acknowledgement) · **DPDP data-rights** (submit + lifecycle
+ honest erasure plan) · **AI** (assistant ask + suggestions accept/reject + anomaly triage) · **offline
"Pending Sync" queue**.

Each screen renders loading/error/empty/success, surfaces the backend's RFC-7807 detail, and enforces
the product rules in the UI (approval blocked until reconciled; "filed" only with an acknowledgement;
maker-checker approve/reject; AI shows confidence; erasure retains financial/tax/KYC).

**Also done (increment 4):** SVG charts (cashflow), i18n (en/hi, switchable), biometric step-up for
high-value payouts, and Detox e2e scaffolding (login + receive).

**Also done (increment 5):** removed the mock prototype; added backend **invoice-list** and
**payout-list** endpoints; **customer / vendor+beneficiary pickers** (real masters instead of raw
IDs) via `EntityPicker`; **Invoices** and **Payouts** list screens; **push-notification** registration
(expo-notifications, fail-soft on web).

**Also done (increment 6):** notification-service **device-token registration** — full loop closed:
`POST/DELETE/GET /api/v1/notifications/devices` (idempotent per token, soft-revoke) + service/tests;
the app now persists its push token to the backend on the dashboard (`syncPushRegistration`) and
unregisters it on logout while the session is still valid.

**Next increments:** more localised strings, in-app notification centre, and running the Detox suite
in CI.
```
