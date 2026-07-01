#!/usr/bin/env bash
# Start the PEB local infrastructure stack and wait for core services to be healthy.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/infra/docker/compose/docker-compose.yml"

echo "▶ Starting PEB local stack..."
docker compose -f "$COMPOSE" up -d

echo "▶ Waiting for Postgres, Redis, Redpanda to report healthy..."
for svc in postgres redis redpanda; do
  printf "  - %s " "$svc"
  for _ in $(seq 1 30); do
    status="$(docker inspect -f '{{.State.Health.Status}}' "peb-local-${svc}-1" 2>/dev/null || echo starting)"
    if [ "$status" = "healthy" ]; then echo "✓"; break; fi
    sleep 2
    printf "."
  done
done

echo ""
echo "✓ Stack up. Endpoints:"
echo "   Postgres   localhost:5432   (peb/peb)"
echo "   Redis      localhost:6379"
echo "   Kafka API  localhost:19092  (Redpanda)"
echo "   MinIO      localhost:9000   console :9001 (peb/peb-secret)"
echo "   Keycloak   localhost:8083   (admin/admin)"
echo "   OpenSearch localhost:9200"
echo "   ClickHouse localhost:8123"
echo "   Grafana    localhost:3000   Prometheus :9090  Tempo :3200  Loki :3100"
echo ""
echo "Next: cd backend && ./gradlew :identity-service:bootRun --args='--spring.profiles.active=local'"
