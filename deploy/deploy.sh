#!/usr/bin/env bash
# Run this on the VPS from the repo checkout (e.g. /opt/checkpointd).
# Pulls the latest main, rebuilds the images, and restarts the stack.
# Flyway migrations run automatically on API startup — no manual step needed.
set -euo pipefail

cd "$(dirname "$0")/.."

git pull origin main

docker compose -f docker-compose.prod.yml --env-file .env.prod build
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

docker image prune -f
