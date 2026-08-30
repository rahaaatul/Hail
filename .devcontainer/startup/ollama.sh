#!/usr/bin/env bash

set -euo pipefail

LOG_FILE="${HOME}/.ollama/ollama-serve.log"

mkdir -p "$(dirname "${LOG_FILE}")"

if pgrep -f "ollama serve" >/dev/null 2>&1; then
    echo "[ollama.sh] ollama serve is running; stopping it..."
    pkill -f "ollama serve" || true
    for _ in $(seq 1 20); do
        pgrep -f "ollama serve" >/dev/null 2>&1 || break
        sleep 0.5
    done
fi

echo "[ollama.sh] starting ollama serve..."
setsid bash -c "exec ollama serve >>'${LOG_FILE}' 2>&1" &

for _ in $(seq 1 20); do
    if pgrep -f "ollama serve" >/dev/null 2>&1; then
        echo "[ollama.sh] ollama serve is up (log: ${LOG_FILE})"
        exit 0
    fi
    sleep 0.5
done

echo "[ollama.sh] ollama serve failed to start; check ${LOG_FILE}" >&2
exit 1
