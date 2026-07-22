#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "$0")/.." && pwd)"
environment_file="${1:-$repository_root/infra/compose/.env}"

if [[ ! -f "$environment_file" ]]; then
  echo "Missing environment file: $environment_file" >&2
  echo "Start from infra/compose/.env.example and provide deployment secrets." >&2
  exit 1
fi

cd "$repository_root/infra/compose"
docker compose --env-file "$environment_file" up -d --build
