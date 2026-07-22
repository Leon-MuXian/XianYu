#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "$0")/.." && pwd)"
environment_file="${SEREN_MEET_API_ENV_FILE:-$repository_root/apps/api/.env}"

if [[ ! -f "$environment_file" ]]; then
  echo "Missing API environment file: $environment_file" >&2
  echo "Start from apps/api/.env.example and provide local secrets." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$environment_file"
set +a

cd "$repository_root/apps/api"
mvn spring-boot:run
