#!/usr/bin/env bash
#
# tg_body_test.sh — regression tests for the Telegram caption escaper.
#
# Usage: bash .github/scripts/tg_body_test.sh
# Exit:  0 = every assertion passed, 1 = at least one failure
#
# Why this exists: escape_html is the only thing standing between an
# attacker-supplied PR title and markup injection into the public release
# channel, and its substitutions have silently been no-ops before — one variant
# even carried a bash parse error that emptied every caption while the workflow
# step stayed green. `bash -n` plus the round-trips below make both shapes fail
# loudly instead of shipping.
#
# The function is extracted from the committed script rather than copied here,
# so this exercises what actually runs. Plain bash on purpose: no bats (not
# preinstalled), no network, no dependency beyond coreutils.
#
# Note: escape_html is deliberately NOT idempotent (& -> &amp; -> &amp;amp;).
# Callers must escape once, into the esc_*-prefixed copies the script keeps.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly SCRIPT_DIR
readonly SCRIPT="${SCRIPT_DIR}/tg_body.sh"

passed=0
failed=0

pass() {
  passed=$((passed + 1))
  printf '  ok   %s\n' "$1"
}

fail() {
  failed=$((failed + 1))
  printf '  FAIL %s\n' "$1"
  printf '         expected: %s\n' "$2"
  printf '         actual:   %s\n' "$3"
}

# assert_escape <description> <input> <expected>
assert_escape() {
  local desc="$1" input="$2" expected="$3" actual
  actual="$(escape_html "${input}")"
  if [[ "${actual}" == "${expected}" ]]; then
    pass "${desc}"
  else
    fail "${desc}" "${expected}" "${actual}"
  fi
}

summary() {
  printf '\n'
  if [[ "${failed}" -eq 0 ]]; then
    printf 'PASS: %d/%d assertions\n' "${passed}" "$((passed + failed))"
  else
    printf 'FAIL: %d of %d assertions failed\n' "${failed}" "$((passed + failed))"
  fi
}

echo "tg_body_test.sh: checking $(basename "${SCRIPT}")"
echo

# --- 1. Syntax ---------------------------------------------------------------
# Would have caught the parse error that made every caption empty (#113).
echo "==> syntax"
syntax_out="$(bash -n "${SCRIPT}" 2>&1)"
syntax_rc=$?
if [[ "${syntax_rc}" -eq 0 ]]; then
  pass "tg_body.sh parses (bash -n)"
else
  fail "tg_body.sh parses (bash -n)" "exit 0" "exit ${syntax_rc}: ${syntax_out}"
  summary
  exit 1
fi

upload_out="$(bash -n "${SCRIPT_DIR}/upload.sh" 2>&1)"
upload_rc=$?
if [[ "${upload_rc}" -eq 0 ]]; then
  pass "upload.sh parses (bash -n)"
else
  fail "upload.sh parses (bash -n)" "exit 0" "exit ${upload_rc}: ${upload_out}"
fi

# --- 2. Load the shipped function --------------------------------------------
echo
echo "==> loading escape_html from the shipped script"
extracted="$(sed -n '/^escape_html() {/,/^}/p' "${SCRIPT}")"
if [[ -z "${extracted}" ]]; then
  fail "escape_html found in tg_body.sh" "an escape_html() definition" "nothing extracted"
  summary
  exit 1
fi

eval "${extracted}"
if declare -F escape_html >/dev/null 2>&1; then
  pass "escape_html extracted and defined"
else
  fail "escape_html extracted and defined" "function escape_html" "missing after eval"
  summary
  exit 1
fi

# --- 3. Escaping round-trips -------------------------------------------------
echo
echo "==> escaping"
assert_escape "ampersand is replaced" 'hello & world' 'hello &amp; world'
assert_escape "double quote is replaced (the #113 case)" 'a"b' 'a&quot;b'
assert_escape "already-escaped entity is not double-escaped" '&quot;' '&amp;quot;'
assert_escape "less-than is replaced" '<' '&lt;'
assert_escape "greater-than is replaced" '>' '&gt;'
assert_escape "ampersand is escaped before angle brackets (&lt; stays intact)" 'a &lt; b' 'a &amp;lt; b'
assert_escape "all metacharacters together" '&<>"' '&amp;&lt;&gt;&quot;'
assert_escape "realistic PR title" \
  'fix(ui): support <b> tags & "smart" quotes' \
  'fix(ui): support &lt;b&gt; tags &amp; &quot;smart&quot; quotes'
assert_escape "markup injection attempt" \
  '<a href="https://evil.example">click me</a>' \
  '&lt;a href=&quot;https://evil.example&quot;&gt;click me&lt;/a&gt;'
assert_escape "empty input stays empty" '' ''

# --- 4. Caption interpolation ------------------------------------------------
# The script keeps raw values intact and escapes into esc_*-prefixed copies, so
# the heredoc must only ever interpolate the esc_ ones. A raw interpolation is
# unescaped markup in a public channel.
echo
echo "==> caption template"
emitted="$(sed -n '/^cat <<EOF$/,/^EOF$/p' "${SCRIPT}")"
if [[ -z "${emitted}" ]]; then
  fail "caption heredoc found in tg_body.sh" "a cat <<EOF block" "nothing extracted"
else
  # Single quotes are deliberate: ${...} here is the literal text the extracted
  # heredoc contains, not a parameter expansion of this script.
  # shellcheck disable=SC2016
  raw_interp="$(grep -o '\${[A-Za-z_][A-Za-z0-9_]*}' <<<"${emitted}" | grep -v '\${esc_' || true)"
  if [[ -n "${emitted}" ]] && [[ -z "${raw_interp}" ]]; then
    pass "heredoc interpolates only esc_*-prefixed values"
  else
    fail "heredoc interpolates only esc_*-prefixed values" \
      "no non-esc_ interpolation" "$(tr '\n' ' ' <<<"${raw_interp}")"
  fi
fi

summary
[[ "${failed}" -eq 0 ]]
