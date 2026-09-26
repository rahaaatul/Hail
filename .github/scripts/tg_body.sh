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
# Requires (must be on PATH):
#   git     — refs, hashes and the commit subject
#   sed     — versionName extraction from app/build.gradle.kts
#   head    — keeps the first match when the file declares several
#   dirname — locates the repository root from $0
#   curl    — HARD dependency for the PR title lookup, taken whenever
#             PR_NUMBER and GH_TOKEN are both set. This is the call hardened
#             below (--fail, --connect-timeout, --max-time, --retry), so a
#             runner without it cannot build a real caption at all: the lookup
#             is skipped, curl's own diagnostics reach stderr and the caption
#             falls back to the local commit subject.
#
# Optional:
#   jq      — parses the PR title API response. Preinstalled on
#             ubuntu-latest. Without it the API call is skipped, a diagnostic
#             is written to stderr and the caption falls back to the local
#             commit subject. An enhancement, not a requirement: the caption
#             is still correct, just less informative. Not silently degraded:
#             see the guard below.
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
# & must be replaced first: the later substitutions introduce ampersands of
# their own, and escaping those again would corrupt the output.
escape_html() {
  local str="$1"
  str="${str//&/&amp;}"
  str="${str//</&lt;}"
  str="${str//>/&gt;}"
  str="${str//\"/&quot;}"
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
#
# jq is preinstalled on ubuntu-latest, but guard for it: the API returns JSON,
# and without a real parser we would be back to scraping it by hand — which is
# what truncated titles containing a double quote. Skipping the call outright
# keeps the degradation explicit instead of shipping a silently mangled
# changelog, and the commit-subject fallback below still escapes correctly.
#
# The diagnostic goes to stderr, not a ::warning:: annotation: GitHub Actions
# reads workflow commands from a step's *stdout*, and this script's stdout is
# the caption, so annotating there would corrupt the message. upload.sh
# deliberately does not suppress this script's stderr, so the line below is
# visible in the workflow log.
#
# Note: `set -o pipefail` is on but `set -e` is not, so a failing curl or jq
# leaves pr_title empty and control simply continues to the fallback. curl's
# stderr is likewise left attached: with `-sS` a --fail exit 22, a --max-time
# exit 28, a DNS or TLS error is otherwise indistinguishable from success.
subject=""
if [[ -n "${PR_NUMBER:-}" && -n "${GH_TOKEN:-}" ]]; then
  if ! command -v jq >/dev/null 2>&1; then
    echo "tg_body.sh: jq not found, skipping PR title lookup" >&2
  else
    # --fail/--connect-timeout/--max-time keep an erroring or hung API
    # endpoint from stalling the build until the job-level timeout, and
    # --retry rides out a transient 5xx/connection reset so the fallback is
    # not taken for a blip.
    pr_title="$(curl -sS --fail --connect-timeout 5 --max-time 15 \
      --retry 2 --retry-delay 1 \
      -H "Authorization: token ${GH_TOKEN}" \
      "https://api.github.com/repos/${REPO}/pulls/${PR_NUMBER}" \
      | jq -r '.title // empty')"
    if [[ -n "${pr_title}" ]]; then
      subject="${pr_title}"
    fi
  fi
fi

# Fallback to local commit subject
if [[ -z "${subject}" ]]; then
  subject="$(git log -1 --format='%s' 2>/dev/null || echo "No changes")"
fi

# Strip conventional-commit prefix: fix:, feat(scope):, chore(deps):, etc.
subject="${subject#*: }"

# --- Escape -----------------------------------------------------------------
# Telegram parses this caption as HTML, so every value interpolated into the
# body below has to be escaped — not just the changelog subject. PR titles and
# branch names are attacker-influenced, and an unescaped < > & or " would let
# them inject markup and links into the channel.
#
# Escape into esc_*-prefixed copies and leave the raw values intact.
# escape_html is not idempotent (& -> &amp; -> &amp;amp;), so overwriting the
# original in place makes the escaping step order-dependent and invisible at
# the call site: a second escape anywhere later corrupts the caption with no
# error. With separate esc_* variables a double escape is a visible mistake.
esc_branch="$(escape_html "${branch}")"
esc_version_name="$(escape_html "${version_name}")"
esc_subject="$(escape_html "${subject}")"
esc_commit_url="$(escape_html "${commit_url}")"
esc_short_hash="$(escape_html "${short_hash}")"

# --- Emit -------------------------------------------------------------------

# Only esc_*-prefixed variables are safe to interpolate below — they are the
# escaped ones. Reaching for a raw variable here would inject unescaped markup
# from an attacker-influenced PR title into the release channel.
cat <<EOF
<b>Branch</b>
<blockquote>${esc_branch}</blockquote>

<b>Version</b>
<blockquote>${esc_version_name}</blockquote>

<b>Changelog</b>
<blockquote>${esc_subject}</blockquote>

<b>Learn more</b>
<blockquote><a href="${esc_commit_url}">${esc_short_hash}</a></blockquote>
EOF