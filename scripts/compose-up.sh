#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../infra/compose"
docker compose up -d postgres
