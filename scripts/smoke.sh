#!/usr/bin/env bash
# Sprint 0 smoke test: verifies identity-service health/ping directly and via the gateway.
set -euo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:8080}"
IDENTITY="${IDENTITY_URL:-http://localhost:8081}"
fail=0

check() {
  local name="$1" url="$2" expect="$3"
  printf "  %-40s " "$name"
  if curl -fsS "$url" | grep -q "$expect"; then echo "✓"; else echo "✗ ($url)"; fail=1; fi
}

echo "▶ Smoke test"
check "identity /actuator/health"     "$IDENTITY/actuator/health" '"status":"UP"'
check "identity /api/v1/ping"         "$IDENTITY/api/v1/ping"      '"status":"ok"'
check "gateway  /actuator/health"     "$GATEWAY/actuator/health"   '"status":"UP"'
check "gateway  -> identity ping"     "$GATEWAY/api/v1/ping"       '"service":"identity-service"'

if [ "$fail" -eq 0 ]; then echo "✓ All smoke checks passed"; else echo "✗ Smoke test failed"; exit 1; fi
