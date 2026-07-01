# vendor-service

Owns vendor/supplier profiles and their payout bank accounts for a tenant.

- Java 21, Spring Boot 3.3, Gradle. HTTP port `8084`. Database `vendor_db`.
- Resource server: validates access tokens from `identity-service` via its JWKS.
- Sensitive fields (mobile, email, GSTIN, account number, IFSC, UPI) are encrypted at rest
  (`EncryptedStringConverter`). Account numbers also carry an HMAC blind index for per-vendor
  de-duplication without exposing plaintext.

## OCR bank-review gate (product rule #7)

OCR results must be user-reviewed before sensitive bank details are usable.

A bank account — whether captured by OCR or entered manually — is **always** created with status
`PENDING_REVIEW` and is **not usable for payouts** in that state. A user must explicitly act on it:

- `POST /api/v1/vendors/{id}/bank-accounts` — saves the account `PENDING_REVIEW`. **No**
  `VENDOR_BANK_DETAILS_CHANGED` event is emitted, because the details are not yet trusted.
- `POST /api/v1/vendors/{id}/bank-accounts/{baId}/confirm` — a user reviews and approves the
  details; status becomes `VERIFIED`, `reviewed_by`/`reviewed_at` are stamped, and
  `VENDOR_BANK_DETAILS_CHANGED` is emitted so downstream services may rely on the account.
- `POST /api/v1/vendors/{id}/bank-accounts/{baId}/reject` — a user rejects mis-read/incorrect
  details; status becomes `REJECTED` and it stays unusable. No event is emitted.

Confirm/reject only apply while the account is `PENDING_REVIEW`; acting on an already-reviewed
account returns `409 CONFLICT`. The reviewer identity is taken from the caller's JWT subject.

Account numbers are masked (last 4 shown) in API responses.

## Events

- `VENDOR_CREATED` — on vendor creation.
- `VENDOR_BANK_DETAILS_CHANGED` — only on bank-account confirmation.
