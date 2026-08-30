#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STARTUP_DIR="${SCRIPT_DIR}/startup"

for script in "${STARTUP_DIR}"/*.sh; do
    [[ -e "${script}" ]] || continue
    [[ -x "${script}" ]] || continue
    echo "[start.sh] running ${script}"
    bash "${script}"
done

echo "[start.sh] done"
