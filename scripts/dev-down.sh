#!/usr/bin/env bash
# Stop the PEB local stack. Pass --volumes to also wipe data.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE="$ROOT/infra/docker/compose/docker-compose.yml"

if [ "${1:-}" = "--volumes" ]; then
  echo "▶ Stopping stack and removing volumes..."
  docker compose -f "$COMPOSE" down -v
else
  echo "▶ Stopping stack (data preserved)..."
  docker compose -f "$COMPOSE" down
fi
