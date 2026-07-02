# notification-service

Templated, multi-channel messaging for the PEB platform (port 8093, DB `notification_db`).

## What it does

- **Channels**: SMS, email, push, and WhatsApp-ready (WhatsApp is wired as a channel; live BSP
  integration is gated on approval). Dev environments use stub providers; real gateway adapters
  (SMS gateway, SMTP/ESP, FCM/APNs, WhatsApp BSP) replace them per environment.
- **Template engine**: per-tenant, per-channel templates with `{{placeholder}}` substitution in
  subject and body.
- **Reminders**: D-3 / D-1 / D-day payment reminders scheduled off a due date. A daily scheduled
  sweep (default 08:00 IST) fires all reminders whose send date has arrived, resolving the tenant
  per reminder row.
- **Delivery status + retry**: sends are retried up to `peb.notification.max-retries`. A message is
  marked `SENT` only when a provider accepts it, and `DELIVERED` only on a provider receipt.
- **Never delivered without provider ack**: delivery is confirmed exclusively via the provider
  delivery-receipt webhook (`POST /api/v1/webhooks/notifications/{provider}`) — the engine never
  claims delivery without acknowledgement.

## Key endpoints

- `POST /api/v1/notification-templates`, `GET /api/v1/notification-templates`
- `POST /api/v1/notifications/send`, `GET /api/v1/notifications`
- `POST /api/v1/reminders`, `GET /api/v1/reminders?sourceRef=...`
- `POST /api/v1/webhooks/notifications/{provider}` (public; provider-signed in production)
