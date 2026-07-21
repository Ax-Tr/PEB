#!/usr/bin/env bash
# Smoke test: verifies all 4 services are up and responding via the gateway.
set -euo pipefail

GATEWAY="${GATEWAY_URL:-http://localhost:8080}"
IDENTITY="${IDENTITY_URL:-http://localhost:8081}"
BUSINESS="${BUSINESS_URL:-http://localhost:8082}"
FINANCE="${FINANCE_URL:-http://localhost:8083}"
fail=0

check() {
  local name="$1" url="$2" expect="$3"
  printf "  %-45s " "$name"
  if curl -fsS --max-time 5 "$url" | grep -q "$expect"; then echo "✓"; else echo "✗ ($url)"; fail=1; fi
}

echo "▶ Smoke test (consolidated 4-service architecture)"

# Direct service checks
check "identity /actuator/health"       "$IDENTITY/actuator/health" '"status":"UP"'
check "identity /api/v1/ping"           "$IDENTITY/api/v1/ping"     '"status":"ok"'
check "business /actuator/health"       "$BUSINESS/actuator/health" '"status":"UP"'
check "business /api/v1/ping"           "$BUSINESS/api/v1/ping"     '"service":"business-service"'
check "finance  /actuator/health"       "$FINANCE/actuator/health"  '"status":"UP"'
check "finance  /api/v1/ping"           "$FINANCE/api/v1/ping"      '"service":"finance-service"'

# Gateway routing checks
check "gateway  /actuator/health"       "$GATEWAY/actuator/health"  '"status":"UP"'
check "gateway  -> identity ping"       "$GATEWAY/api/v1/ping"      '"service":"identity-service"'

if [ "$fail" -eq 0 ]; then echo "✓ All smoke checks passed"; else echo "✗ Smoke test failed"; exit 1; fi
