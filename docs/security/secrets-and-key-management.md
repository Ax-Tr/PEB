# Secrets & Key Management (Sprint 16)

## Secrets handling

- **No secrets in source or images.** All secrets (DB creds, JWT signing keys, field-encryption
  keys, provider API keys, webhook HMAC secrets) are injected at runtime from the environment /
  secret manager (e.g. Vault / cloud secret manager). Local/dev uses env vars; prod uses the manager
  with short-lived leases where supported.
- **`.gitignore`** excludes key material (`*.jks`, `*.p8`, `*.p12`, `*.key`, `*.mobileprovision`).
- **Rotation cadence:** JWT signing keys and provider keys rotate on a fixed schedule and on
  suspected compromise (see incident runbook). Rotation must be **zero-downtime**.

## Field-encryption key rotation — `KeyRing`

`common-libraries` provides `KeyRing`, a versioned set of AES-256-GCM keys enabling non-breaking
rotation:

- New data is encrypted with the **active** key version; ciphertext is tagged `v{n}:...`.
- Every prior key version is retained so any existing ciphertext still decrypts.
- Legacy unversioned ciphertext (from the original single-key `AesGcmCipher`) decrypts as version 0,
  allowing gradual migration.
- `needsReEncryption(token)` flags values written under an old key so a background sweep can
  re-encrypt them, after which the retired key can be removed.

**Rotation procedure**
1. Add the new key at `activeVersion + 1` (`KeyRing.withNewActiveKey`) and deploy. New writes use it;
   all reads still work.
2. Run the re-encryption sweep (read → `KeyRing.decrypt` → `KeyRing.encrypt` → write) for values
   where `needsReEncryption` is true.
3. Once the sweep completes, retire the old key version from the ring.

The existing `EncryptedStringConverter` (single-key `FieldCrypto`) is unchanged to avoid a big-bang
data migration; the documented rollout is to switch the converter to a `KeyRing`-backed cipher, which
is backward-compatible with already-written unversioned ciphertext.

## Per-tenant key strategy — DECISION

**Decision:** adopt **envelope encryption with a per-tenant Data Encryption Key (DEK), wrapped by a
shared KMS-held Key Encryption Key (KEK)**, rather than a distinct KMS key per tenant.

- **Why not KMS-key-per-tenant:** thousands of MSME tenants → unmanageable KMS key sprawl, quota and
  cost pressure, slow per-request KMS calls.
- **Chosen model:** one KMS KEK (rotated in KMS); each tenant has its own 32-byte DEK generated on
  tenant creation, stored **wrapped** by the KEK. The DEK is unwrapped on demand and cached briefly
  in memory. Field encryption uses the tenant DEK via `KeyRing` (DEK versions rotate independently of
  the KEK).
- **Blast radius:** compromise of one tenant's DEK exposes only that tenant; KEK rotation re-wraps
  DEKs without touching ciphertext.
- **Status:** decision recorded; `KeyRing` provides the versioning primitive. Wiring DEK
  provisioning/unwrapping to a live KMS is an infrastructure task (KMS not available in this
  environment) and is tracked as a follow-up.
