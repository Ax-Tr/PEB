# analytics-service

Event-fed OLAP read-model powering MSME dashboards.

## What it does

- **Projections from events only** — consumes invoice/payment/purchase/payout events and maintains a
  denormalised read-model. It **never queries any OLTP service's database**; its only inputs are
  domain events. Production target for the aggregates is **ClickHouse**; PostgreSQL (`analytics_db`)
  is used now because it is the only engine verifiable in this environment.
- **Dashboards** (all read-only GET)
  - *P&L / margins* — revenue, direct cost, operating expense, gross/net profit, gross/net margin %.
  - *Cashflow* — per-period inflows/outflows, net movement, running closing balance.
  - *Receivables / payables aging* — 0–30 / 31–60 / 61–90 / 90+ buckets.
  - *Product profitability* — products ranked by profit and margin.
  - *Freshness indicator* — because analytics is **eventually consistent**, every stream reports how
    current its projection is (FRESH / STALE / NO_DATA + lag seconds).

## API

Base path `/api/v1/analytics`. All endpoints are GET and require a valid JWT (resource server); any
authenticated tenant user may read. Port **8097**, database `analytics_db`.

- `GET /pnl?year=&month=`
- `GET /receivables-aging?asOf=YYYY-MM-DD` (`asOf` optional; defaults to today, Asia/Kolkata)
- `GET /payables-aging?asOf=YYYY-MM-DD`
- `GET /cashflow`
- `GET /product-profitability?year=&month=`
- `GET /freshness`

## Topics consumed

`invoice.events` (`INVOICE_GENERATED`), `purchase.events` (`PURCHASE_BILL_CREATED`,
`EXPENSE_APPROVED`), `payment.events` (`PAYMENT_RECEIVED`), `payout.events`
(`VENDOR_PAYMENT_COMPLETED`). Consumption is idempotent (ingest is keyed on the aggregate's natural
key); the stream watermark is advanced per handled event to drive `/freshness`.

## Documented gaps

- **Periods are ingest-time (Asia/Kolkata)** — the event envelope carries no business/occurred date,
  so year/month and aging reference dates are derived at ingest time. A future event-schema
  enrichment should carry the real business date.
- **Payment → invoice linkage is not available from events** — so invoice `amount_paid` stays 0 and
  receivables age by invoice date rather than settlement.
- **Product profitability is empty until invoice events carry line-level detail** (product id,
  revenue, cost). The ranking is correct regardless; the feeding read-model is simply empty for now.
