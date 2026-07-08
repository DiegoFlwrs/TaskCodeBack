#!/usr/bin/env bash
set -euo pipefail

ROOT="/opt/taskcode"
BRANCH="${DEPLOY_BRANCH:-main}"
COMPOSE_FILE="TaskCodeBack/deploy/docker-compose.yml"
ENV_FILE=".env"
SERVICES="${1:-}"

log() { echo "[deploy $(date '+%H:%M:%S')] $*"; }

cd "$ROOT"

log "Actualizando TaskCodeBack ($BRANCH)..."
cd TaskCodeBack
git fetch origin
git checkout "$BRANCH"
git pull origin "$BRANCH"
cd "$ROOT"

log "Actualizando taskcodefront ($BRANCH)..."
cd taskcodefront
git fetch origin
git checkout "$BRANCH"
git pull origin "$BRANCH"
cd "$ROOT"

if [[ -n "$SERVICES" ]]; then
  log "Rebuild: $SERVICES"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build $SERVICES
else
  log "Rebuild: todos los servicios"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build
fi

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
log "Deploy completado."
