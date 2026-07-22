#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "$0")/.." && pwd)"
environment_file="${SEREN_MEET_OWNER_ENV_FILE:-$repository_root/apps/miniprogram-owner/.env}"

if [[ ! -f "$environment_file" ]]; then
  echo "Missing owner mini program environment file: $environment_file" >&2
  echo "Start from apps/miniprogram-owner/.env.example and provide a device-accessible API URL." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$environment_file"
set +a

if [[ -z "${TARO_APP_API_BASE_URL:-}" ]]; then
  echo "TARO_APP_API_BASE_URL is required for a real-device debug build." >&2
  exit 1
fi

if [[ "$TARO_APP_API_BASE_URL" =~ ^https?://(127\.|localhost)(:|/|$) ]]; then
  echo "TARO_APP_API_BASE_URL must be reachable from the phone, not a loopback address." >&2
  exit 1
fi

cd "$repository_root/apps/miniprogram-owner"
exec npx taro build --type weapp
