#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../apps/api"
mvn spring-boot:run
