#!/usr/bin/env bash
# Shared helpers. Sourced, never executed.

set -euo pipefail

step() { printf '\n==> %s\n' "$*"; }
note() { printf '::notice::%s\n' "$*"; }
warn() { printf '::warning::%s\n' "$*"; }
die()  { printf '::error::%s\n' "$*" >&2; exit 1; }

require_env() {
  local name
  for name in "$@"; do
    [ -n "${!name:-}" ] || die "Missing required environment variable: ${name}"
  done
}

repo_root_path() { cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd; }
repo_root() { cd "$(repo_root_path)"; }

single_match() {
  local pattern="$1" base found
  # Scoped to the build output rather than the whole checkout. Searching from
  # the repository root resolves the APK in filesystem order, so a second match
  # anywhere - a per-ABI output, a leftover from an earlier step in the same
  # job - could hand back the wrong artifact to rename, upload and publish.
  base="${2:-$(repo_root_path)/app/build/outputs/apk}"
  found="$(find "$base" -name "$pattern" -type f -print -quit 2>/dev/null || true)"
  [ -n "$found" ] || die "No file matching '$pattern' was produced by the build"
  printf '%s' "$found"
}
