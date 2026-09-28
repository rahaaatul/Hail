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

build_tools_dir() {
  local dir
  dir="$(ls -d "${ANDROID_HOME:-/opt/android-sdk}"/build-tools/* 2>/dev/null | sort -V | tail -1 || true)"
  [ -n "$dir" ] || die "No Android build-tools found"
  printf '%s' "$dir"
}

single_match() {
  local pattern="$1" found root
  root="$(repo_root_path)"
  found="$(find "$root" -path "$root/.git" -prune -o -name "$pattern" -type f -print -quit)"
  [ -n "$found" ] || die "No file matching '$pattern' was produced by the build"
  printf '%s' "$found"
}
