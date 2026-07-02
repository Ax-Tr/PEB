#!/usr/bin/env bash
# PEB staging/production smoke test — golden-path health + a read path per service family.
# Usage: BASE_URL=https://staging.example TOKEN=<jwt> ./smoke.sh
# Exit non-zero on the first failed check (suitable as a deploy gate / canary check).
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-}"
AUTH=(-H "Authorization: Bearer ${TOKEN}")
fail() { echo "SMOKE FAIL: $1" >&2; exit 1; }

echo "== PEB smoke against ${BASE_URL} =="

# 1) Gateway + service health (no auth).
curl -fsS "${BASE_URL}/actuator/health" >/dev/null || fail "gateway health"
curl -fsS "${BASE_URL}/api/v1/ping" >/dev/null || fail "identity ping"

# 2) Security headers present at the edge (OWASP hardening).
hdrs="$(curl -fsSI "${BASE_URL}/api/v1/ping")"
echo "$hdrs" | grep -qi "X-Content-Type-Options: nosniff" || fail "missing nosniff header"
echo "$hdrs" | grep -qi "Content-Security-Policy" || fail "missing CSP header"

# 3) Authn required: a protected route without a token must be 401.
code="$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}/api/v1/analytics/freshness")"
[ "$code" = "401" ] || fail "protected route not enforcing auth (got $code)"

if [ -z "$TOKEN" ]; then
  echo "No TOKEN provided — skipping authenticated read checks (health/security/authn OK)."
  echo "SMOKE PASS (unauthenticated subset)"; exit 0
fi

# 4) Authenticated golden-path reads across service families (expect 2xx).
check_get() {
  local path="$1"
  local c
  c="$(curl -s -o /dev/null -w '%{http_code}' "${AUTH[@]}" "${BASE_URL}${path}")"
  [[ "$c" =~ ^2 ]] || fail "GET ${path} -> ${c}"
  echo "ok  GET ${path} (${c})"
}
check_get "/api/v1/analytics/freshness"
check_get "/api/v1/analytics/cashflow"
check_get "/api/v1/compliance/reports"
check_get "/api/v1/ai/suggestions"
check_get "/api/v1/privacy/requests"
check_get "/api/v1/collaboration/invites"
check_get "/api/v1/audit/exports"

echo "SMOKE PASS"
