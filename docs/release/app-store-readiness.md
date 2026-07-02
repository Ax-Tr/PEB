# App-Store Readiness — Mobile (Sprint 18)

Bare React Native + TypeScript, **Android priority** (then iOS). Checklist for Play Store (and App
Store) submission. Items needing signed builds / store consoles are ops tasks flagged *[deploy]*.

## Build & signing
- [ ] Release build config separate from debug; ProGuard/R8 minify + resource shrink on. *[deploy]*
- [ ] Android app signing via Play App Signing; upload key in the secret manager, not the repo. *[deploy]*
- [ ] Versioning: `versionCode`/`versionName` bumped from CI; matches backend `/api/v1` contract.

## Security (MASVS-aligned)
- [ ] No secrets in the JS bundle or native config; API base URL per environment.
- [ ] Tokens in Keychain/Keystore (secure storage); never in AsyncStorage plaintext.
- [ ] Certificate pinning to the gateway; reject on pin mismatch.
- [ ] Biometric unlock for app open + step-up on sensitive actions (payout, bank change).
- [ ] Root/jailbreak signal handled (warn/limit); screenshot masking on PII screens.

## Privacy & compliance (Play Data safety / DPDP)
- [ ] Data-safety form: declare data collected (contact, financial), encryption in transit, and the
      in-app data-deletion path (maps to privacy-service DSR erasure).
- [ ] Privacy policy URL live; consent captured on first run; marketing consent separate + revocable.
- [ ] Account/data-deletion request reachable from the app (Play requirement) → privacy-service.

## UX quality (per coding standards §2)
- [ ] Every screen has loading / empty / error / success states.
- [ ] Accessibility labels; 44×44 tap targets; dynamic type; i18n (en first).
- [ ] Offline: encrypted local store + "Pending Sync" queue; **payment confirmation requires online**;
      conflict detection on sync.
- [ ] OCR/bank-detail capture always shows extracted values for **user review before saving**.
- [ ] AI outputs display their confidence; low-confidence prompts a human decision.

## Store assets *[deploy]*
- [ ] Icons, feature graphic, screenshots (phone + tablet), short/long description, category = Finance.
- [ ] Content rating questionnaire; target API level meets Play's current requirement.

## Functional verification
- [ ] Deep links resolve (invoice/payment links); push notifications deliver; biometrics verified on
      device.
- [ ] Golden path works on a low-end Android device on a slow network.

**Status:** the checklist is the release gate. The RN app scaffold and these requirements are defined;
signed store builds, console submission, and on-device push/biometric verification are *[deploy]* tasks
performed with the store consoles and signing keys, which are not available in this repo sandbox.
