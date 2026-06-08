#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../apps/admin-web"
npm run dev
