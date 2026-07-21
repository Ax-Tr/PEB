#!/usr/bin/env bash
# Start the PEB local stack (infra + 4 application services) and wait for everything to be healthy.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/infra/docker/compose/docker-compose.yml"

echo "▶ Starting PEB local stack (consolidated 4-service architecture)..."
docker compose -f "$COMPOSE" up -d --build

echo "▶ Waiting for core infra to report healthy..."
for svc in postgres redis redpanda; do
  printf "  - %s " "$svc"
  for _ in $(seq 1 30); do
    status="$(docker inspect -f '{{.State.Health.Status}}' "peb-local-${svc}-1" 2>/dev/null || echo starting)"
    if [ "$status" = "healthy" ]; then echo "✓"; break; fi
    sleep 2
    printf "."
  done
done

echo "▶ Waiting for application services..."
for svc in identity-service business-service finance-service api-gateway; do
  printf "  - %s " "$svc"
  for _ in $(seq 1 60); do
    status="$(docker inspect -f '{{.State.Health.Status}}' "peb-local-${svc}-1" 2>/dev/null || echo starting)"
    if [ "$status" = "healthy" ]; then echo "✓"; break; fi
    sleep 3
    printf "."
  done
done

echo ""
echo "✓ Stack up. Endpoints:"
echo "   Gateway       http://localhost:8080  (routes to all services)"
echo "   Identity      http://localhost:8081"
echo "   Business      http://localhost:8082"
echo "   Finance       http://localhost:8083"
echo "   Postgres      localhost:5432   (peb/peb) — identity_db, business_db, finance_db"
echo "   Redis         localhost:6379"
echo "   Kafka API     localhost:19092  (Redpanda)"
echo "   MinIO         localhost:9000   console :9001 (peb/peb-secret)"
echo "   Grafana       localhost:3000   Prometheus :9090  Tempo :3200  Loki :3100"
echo ""
echo "Run smoke test: bash scripts/smoke.sh"
