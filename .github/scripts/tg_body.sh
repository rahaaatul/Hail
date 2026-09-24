#!/usr/bin/env bash
#
# tg_body.sh — generate the Telegram caption for a debug/release build.
#
# Usage: bash tg_body.sh
# Output: HTML caption (one block) to stdout
#
# All data is derived from git and the build file — no AI, no debug.md.
#
# Format:
#   <b>Branch</b>
#   <blockquote><branch></blockquote>
#
#   <b>Version</b>
#   <blockquote><version></blockquote>
#
#   <b>Changelog</b>
#   <blockquote><latest-commit-subject></blockquote>
#
#   <b>Learn more</b>
#   <blockquote><a href="<commit-url>"><short-hash></a></blockquote>
#
# Environment:
#   REPO       — "owner/repo" for the commit URL (default: rahaaatul/Hail)
#   PR_NUMBER  — pull request number; when set, the Branch label becomes
#                "PR #<N>" instead of the (detached) HEAD ref
#   GH_TOKEN   — GitHub token for PR title lookup (optional)

set -uo pipefail

cd "$(dirname "$0")/../.."

readonly REPO="${REPO:-rahaaatul/Hail}"

# --- HTML escaping helper ---------------------------------------------------
escape_html() {
  local str="$1"
  str="${str//&/&}"
  str="${str//</<}"
  str="${str//>/>}"
  str="${str//\"/"}"
  printf '%s' "$str"
}

# --- Derive values ----------------------------------------------------------

branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")"

# PR builds run on a detached HEAD checkout, so the branch name resolves to
# "HEAD". When PR_NUMBER is set, label the caption PR #<N> instead.
if [[ -n "${PR_NUMBER:-}" ]]; then
  branch="PR #${PR_NUMBER}"
fi

full_hash="$(git rev-parse HEAD 2>/dev/null || echo "0000000000000000000000000000000000000000")"
short_hash="${full_hash:0:7}"
commit_url="https://github.com/${REPO}/commit/${full_hash}"

version_name="$(sed -n 's/.*versionName\s*=\s*"\([^"]*\)".*/\1/p' app/build.gradle.kts 2>/dev/null | head -1 || echo "unknown")"

# Get changelog: prefer PR title from GitHub API (if PR_NUMBER and GH_TOKEN),
# fall back to local commit subject.
subject=""
if [[ -n "${PR_NUMBER:-}" && -n "${GH_TOKEN:-}" ]]; then
  pr_title="$(curl -sS -H "Authorization: token ${GH_TOKEN}" \
    "https://api.github.com/repos/${REPO}/pulls/${PR_NUMBER}" 2>/dev/null \
    | sed -n 's/.*"title"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"
  if [[ -n "${pr_title}" ]]; then
    subject="${pr_title}"
  fi
fi

# Fallback to local commit subject
if [[ -z "${subject}" ]]; then
  subject="$(git log -1 --format='%s' 2>/dev/null || echo "No changes")"
fi

# Strip conventional-commit prefix: fix:, feat(scope):, chore(deps):, etc.
subject="${subject#*: }"

# Escape HTML for safe embedding in Telegram parse_mode=HTML
subject="$(escape_html "${subject}")"

# --- Emit -------------------------------------------------------------------

cat <<EOF
<b>Branch</b>
<blockquote>${branch}</blockquote>

<b>Version</b>
<blockquote>${version_name}</blockquote>

<b>Changelog</b>
<blockquote>${subject}</blockquote>

<b>Learn more</b>
<blockquote><a href="${commit_url}">${short_hash}</a></blockquote>
EOF

