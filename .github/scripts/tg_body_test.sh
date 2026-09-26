#!/usr/bin/env bash
#
# tg_body_test.sh — regression tests for the Telegram caption escaper.
#
# Runs automatically from .github/scripts/setup.sh, which is the first thing
# the build job and both local wrappers (tg_debug.sh, tg_release.sh) execute.
# A failure there aborts the build, so nothing depends on anyone remembering
# to run this by hand. It can still be run directly for a tighter loop:
#   bash .github/scripts/tg_body_test.sh
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
# preinstalled), no network, no dependency beyond coreutils (grep, sed, sort,
# comm, head).
#
# Three layers, because a round-trip alone is not enough:
#   1. escape_html produces the right bytes for a given input
#   2. the caption heredoc interpolates only esc_* variables
#   3. every esc_* it interpolates is actually assigned from escape_html, and
#      upload.sh still leaves tg_body.sh's stderr attached
# (2) without (3) passes for esc_subject="${subject}": a raw copy still has an
# esc_ prefix, so the prefix check is green while the caption ships unescaped
# markup.
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
#
# [ ... = ... ] rather than [[ ... == ... ]]: `=` is a plain string equality,
# while the right operand of `==` is a glob pattern unless it is quoted. The
# quoting here makes the two equivalent today, but nothing but the quotes is
# holding that line, and the glob fixtures below are the ones that would
# notice if they were dropped.
assert_escape() {
  local desc="$1" input="$2" expected="$3" actual
  actual="$(escape_html "${input}")"
  if [ "${actual}" = "${expected}" ]; then
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
# Glob metacharacters are not HTML metacharacters: they must survive verbatim
# and the comparison must stay exact. A broken escaper leaves * ? and [ alone,
# so these pass either way here — they exist to pin the comparison, not the
# substitution.
assert_escape "glob metacharacters pass through verbatim" \
  'fix(ui): match *args and ? in [a-z] globs' \
  'fix(ui): match *args and ? in [a-z] globs'
assert_escape "glob metacharacters pass through alongside escaped ones" \
  'fix(ui): match *args and ? in [a-z] globs & log the <b> tag' \
  'fix(ui): match *args and ? in [a-z] globs &amp; log the &lt;b&gt; tag'
# The "*" is deliberately glued to the "&". That adjacency is what makes a glob
# comparison swallow a real regression: applying the & substitution twice turns
# the expected "&amp;" into "&amp;amp;", and as a pattern "*&amp;* unexpanded"
# matches "*&amp;amp;* unexpanded" in full, so [[ x == y ]] would report ok for a
# broken escaper. [ x = y ] does not.
assert_escape "ampersand followed by a glob star, compared exactly" \
  'fix: keep &* unexpanded in captions' \
  'fix: keep &amp;* unexpanded in captions'
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

  # The prefix check above is satisfied by any esc_* name, escaped or not:
  # reverting one line to esc_subject="${subject}" keeps it green while the
  # caption ships the raw title. Diff the name sets instead — every name the
  # heredoc interpolates has to be assigned from escape_html.
  #
  # names_on <lines> -> sorted unique leading esc_* names.
  names_on() { grep -o '^esc_[A-Za-z0-9_]*' | sort -u; }
  # Single quotes are deliberate: ${...} here is the literal text the extracted
  # heredoc contains, not a parameter expansion of this script.
  # shellcheck disable=SC2016
  esc_in_heredoc="$(grep -o '\${esc_[A-Za-z0-9_]*}' <<<"${emitted}" | sed 's/^\${//; s/}$//' | sort -u)"
  esc_assigns="$(grep '^esc_[A-Za-z0-9_]*=' "${SCRIPT}" || true)"
  esc_assigned="$(names_on <<<"${esc_assigns}")"
  esc_escaped="$(names_on <<<"$(grep 'escape_html' <<<"${esc_assigns}")")"

  # comm needs newline-terminated, sorted input; sed drops the blank line an
  # empty variable would otherwise contribute.
  in_list() { printf '%s\n' "${1}" | sed '/^$/d'; }
  not_escaped="$(comm -23 <(in_list "${esc_assigned}") <(in_list "${esc_escaped}"))"
  unassigned="$(comm -23 <(in_list "${esc_in_heredoc}") <(in_list "${esc_escaped}"))"

  if [[ -n "${not_escaped}" ]]; then
    fail "every esc_* assignment calls escape_html" \
      "each esc_* assigned from escape_html" "raw copies: $(tr '\n' ' ' <<<"${not_escaped}")"
  else
    pass "every esc_* assignment calls escape_html"
  fi

  if [[ -z "${unassigned}" ]]; then
    pass "every esc_* interpolated in the heredoc is assigned from escape_html"
  else
    fail "every esc_* interpolated in the heredoc is assigned from escape_html" \
      "esc_* = escape_html for $(tr '\n' ' ' <<<"${esc_in_heredoc}")" \
      "not escaped: $(tr '\n' ' ' <<<"${unassigned}")"
  fi
fi

# --- 5. upload.sh wiring -----------------------------------------------------
# tg_body.sh reports its own diagnostics on stderr (no jq, API failure). A
# 2>/dev/null on its invocation throws those away and the caption silently
# degrades, which is the failure mode the #113-era guards were added for.
echo
echo "==> upload.sh wiring"
upload_invoke="$(grep -nE '\$\(.*tg_body\.sh' "${SCRIPT_DIR}/upload.sh" || true)"
if [[ -z "${upload_invoke}" ]]; then
  fail "upload.sh still invokes tg_body.sh" \
    "a \$(... tg_body.sh ...) invocation" 'no invocation found'
elif grep -qE '2>[[:space:]]*/dev/null|2>&1' <<<"${upload_invoke}"; then
  fail "upload.sh leaves tg_body.sh's stderr attached" \
    'no 2>/dev/null and no 2>&1 on the invocation' "${upload_invoke}"
else
  pass "upload.sh leaves tg_body.sh's stderr attached"
fi

summary
[[ "${failed}" -eq 0 ]]
