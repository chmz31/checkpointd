#!/usr/bin/env bash
# Dumps the production Postgres database to a compressed file and prunes
# backups older than RETENTION_DAYS. Intended to run daily via cron on the
# VPS (see the crontab entry set up alongside this script).
set -euo pipefail

REPO_DIR="/opt/checkpointd"
COMPOSE_FILE="$REPO_DIR/docker-compose.prod.yml"
ENV_FILE="$REPO_DIR/.env.prod"
BACKUP_DIR="/var/backups/checkpointd"
RETENTION_DAYS=7
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

mkdir -p "$BACKUP_DIR"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T db \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > "$BACKUP_DIR/checkpointd-$TIMESTAMP.sql.gz"

find "$BACKUP_DIR" -name 'checkpointd-*.sql.gz' -mtime +"$RETENTION_DAYS" -delete

echo "Backup written to $BACKUP_DIR/checkpointd-$TIMESTAMP.sql.gz"
