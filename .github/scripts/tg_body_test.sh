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
# It also has to fail for a reason no assertion here can see. The escaper was
# written with ${var//pat/rep}, which bash 5.2 changed the meaning of (see
# patsub_replacement in tg_body.sh), so this suite was green on every developer's
# bash 5.1 and red on the runner. Nothing recorded the interpreter, so the
# version that validated the escaper was invisible. Hence the version line and
# the 5.2 floor in section 0: a run on anything older is a failed run, not a
# pass.
#
# The function is extracted from the committed script rather than copied here,
# so this exercises what actually runs. Plain bash on purpose: no bats (not
# preinstalled), no network, no dependency beyond coreutils (bash, grep, sed,
# sort, comm, tr, basename).
#
# Four layers, because a round-trip alone is not enough:
#   0. the tools the name-set checks are built from are present at all
#   1. escape_html produces the right bytes for a given input
#   2. the caption heredoc interpolates only esc_* variables
#   3. every esc_* it interpolates is actually assigned from escape_html, and
#      upload.sh still leaves tg_body.sh's stderr attached
# (2) without (3) passes for esc_subject="${subject}": a raw copy still has an
# esc_ prefix, so the prefix check is green while the caption ships unescaped
# markup.
#
# Layer 0, and the non-emptiness preconditions inside layers 2 and 3, exist
# because of the failure mode this file used to have in itself. A `|| true`
# after each grep made *nothing found* indistinguishable from *everything
# correct*: delete the whole escape block and every extracted set is empty, both
# comm -23 differences come out empty, and the suite reported ok having checked
# nothing. An assertion that cannot fail reports protection that does not exist,
# so every extraction below keeps grep's exit status (0 = matched, 1 = valid run
# with no match, >1 = grep itself failed) and must be non-empty before it is
# compared against anything. The esc_* name set is pinned rather than merely
# differenced, so a silently deleted assignment is caught too.
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

# assert_nonempty <description> <value> <rc> <what-empty-means>
#
# The precondition used by every extraction below. `rc` is the exit status of the
# grep that produced <value>: 0 = at least one match, 1 = a valid run with no
# match, >1 = grep failed (unknown option, unreadable file, missing binary).
# `|| true` used to collapse all three into the same empty string, which is the
# difference between "the guard is checking something" and "the guard is
# checking nothing" — so all three are reported distinctly and all of them are
# a failure, because a non-vacuous comparison is the only case worth passing.
assert_nonempty() {
  local desc="$1" value="$2" rc="$3" empty_means="$4"
  if [[ "${rc}" -gt 1 ]]; then
    fail "${desc}" "a non-empty extraction" \
      "extraction failed: grep exited ${rc} (not an empty result)"
  elif [[ -z "${value}" ]]; then
    fail "${desc}" "a non-empty extraction" "${empty_means}"
  else
    pass "${desc}"
  fi
}

# assert_set_equal <description> <expected-names> <actual-names>
#
# Both operands are newline-separated, already sorted by `names_on`, and
# compared as whole strings with [ = ] (not [[ == ]]) so no glob metacharacter
# in a variable name can turn a mismatch into a match. Comparing the sets
# outright is what catches a silently deleted or added assignment: "the
# difference is empty" also holds when the entire escape block is gone.
assert_set_equal() {
  local desc="$1" want="$2" got="$3"
  if [[ "${got}" = "${want}" ]]; then
    pass "${desc}"
  else
    fail "${desc}" "$(tr '\n' ' ' <<<"${want}")" "$(tr '\n' ' ' <<<"${got:-<none>}")"
  fi
}

# --- 0. Tools ----------------------------------------------------------------
# Every name-set comparison in section 4 and the wiring check in section 5 is
# built out of these, and `set -o pipefail` is on while `set -e` is not. A
# missing `grep` or `comm` therefore yields an empty string, not a non-zero
# exit — and an empty string satisfies every "nothing was found" comparison
# downstream. Check them up front so a degraded environment is a loud failure
# here rather than a silent pass in an assertion that reports coverage.
# This also runs before the banner below, which uses basename.
echo "==> tools"
for tool in bash grep sed sort comm tr basename; do
  if command -v "${tool}" >/dev/null 2>&1; then
    pass "tool on PATH: ${tool}"
  else
    fail "tool on PATH: ${tool}" 'on PATH' 'command not found'
  fi
done
if [[ "${failed}" -ne 0 ]]; then
  summary
  exit 1
fi

echo
echo "tg_body_test.sh: checking $(basename "${SCRIPT}")"
echo "tg_body_test.sh: bash ${BASH_VERSION}"
echo

# --- 0. Interpreter ---------------------------------------------------------
# The escaper is validated against bash >= 5.2, the release that introduced
# patsub_replacement. Older interpreters lack the option, so a green run there
# does not prove anything about the runner and must not be mistaken for one.
# The version is printed above so the run log always records which interpreter
# actually validated these assertions.
echo "==> interpreter"
if (( BASH_VERSINFO[0] < 5 || (BASH_VERSINFO[0] == 5 && BASH_VERSINFO[1] < 2) )); then
  fail "bash >= 5.2 (patsub_replacement era)" \
    'bash 5.2 or newer' "bash ${BASH_VERSION}"
  summary
  exit 1
fi
pass "bash >= 5.2 (patsub_replacement era)"

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
#
# Every extraction below keeps grep's exit status instead of discarding it with
# `|| true`. `|| true` made three outcomes indistinguishable: interpolations
# found, none found, and grep itself broken. All three leave the extracted
# string empty, an empty string makes both comm -23 differences empty, and the
# two difference assertions below then reported ok having compared nothing.
echo
echo "==> caption template"
emitted="$(sed -n '/^cat <<EOF$/,/^EOF$/p' "${SCRIPT}")"; sed_rc=$?
if [[ "${sed_rc}" -ne 0 ]]; then
  fail "caption heredoc found in tg_body.sh" "a cat <<EOF block" \
    "sed exited ${sed_rc}"
  summary
  exit 1
elif [[ -z "${emitted}" ]]; then
  fail "caption heredoc found in tg_body.sh" "a cat <<EOF block" "nothing extracted"
  summary
  exit 1
else
  pass "caption heredoc found in tg_body.sh"

  # --- 4a. the heredoc interpolates something, and only esc_* ---------------
  # If the block stops interpolating anything, the prefix check has nothing to
  # police and would pass on a caption that is entirely static text. Assert
  # that the interpolation list is non-empty first, so the check that follows
  # cannot pass for want of input.
  #
  # Single quotes are deliberate: ${...} here is the literal text the extracted
  # heredoc contains, not a parameter expansion of this script.
  # shellcheck disable=SC2016
  all_interp="$(grep -o '\${[A-Za-z_][A-Za-z0-9_]*}' <<<"${emitted}")"; all_interp_rc=$?
  if [[ "${all_interp_rc}" -eq 1 ]]; then
    fail "caption heredoc interpolates at least one value" \
      "one or more \${var} inside the cat <<EOF block" \
      "no \${...} found; the caption would be static text"
  else
    assert_nonempty "caption heredoc interpolates at least one value" \
      "${all_interp}" "${all_interp_rc}" "no \${...} found"
  fi

  # Here grep -v exiting 1 is the *success* case (nothing left after removing
  # the esc_ lines), so the status is read the other way round: >1 is a broken
  # extraction and still a failure, and only an empty result with status 0 or 1
  # is a pass.
  # shellcheck disable=SC2016
  raw_interp="$(printf '%s\n' "${all_interp}" | grep -v '\${esc_')"; raw_interp_rc=$?
  if [[ "${raw_interp_rc}" -gt 1 ]]; then
    fail "heredoc interpolates only esc_*-prefixed values" \
      "no non-esc_ interpolation" "extraction failed: grep exited ${raw_interp_rc}"
  elif [[ -n "${raw_interp}" ]]; then
    fail "heredoc interpolates only esc_*-prefixed values" \
      "no non-esc_ interpolation" "$(tr '\n' ' ' <<<"${raw_interp}")"
  else
    pass "heredoc interpolates only esc_*-prefixed values"
  fi

  # --- 4b. the esc_* name sets are non-empty, then pinned -------------------
  # The prefix check above is satisfied by any esc_* name, escaped or not:
  # reverting one line to esc_subject="${subject}" keeps it green while the
  # caption ships the raw title. Diff the name sets instead — every name the
  # heredoc interpolates has to be assigned from escape_html.
  #
  # names_on <lines> -> sorted unique leading esc_* names. sed rather than
  # `grep -o` on purpose: sed exits 0 on empty input, so an empty result here
  # unambiguously means "no such names", and the preconditions below are the
  # only thing that has to notice that.
  names_on() { sed -n 's/^\(esc_[A-Za-z0-9_]*\).*/\1/p' | sort -u; }

  # shellcheck disable=SC2016
  esc_in_heredoc="$(grep -o '\${esc_[A-Za-z0-9_]*}' <<<"${emitted}" | sed 's/^\${//; s/}$//' | sort -u)"; heredoc_rc=$?
  esc_assigns="$(grep '^esc_[A-Za-z0-9_]*=' "${SCRIPT}")"; assigns_rc=$?

  assert_nonempty "caption heredoc interpolates at least one esc_* variable" \
    "${esc_in_heredoc}" "${heredoc_rc}" \
    "no \${esc_...} in the caption; the escaper is not wired into the caption"
  assert_nonempty "tg_body.sh assigns at least one esc_* variable" \
    "${esc_assigns}" "${assigns_rc}" \
    'no esc_*= assignment found; the escape block was renamed, moved or deleted'

  # The single most important precondition in this file. An empty
  # esc_escaped means nothing at all is assigned from escape_html, i.e. the
  # escaper is unwired — and under the previous `|| true` form that was
  # reported as ok for both difference assertions below.
  esc_escaped_lines="$(printf '%s\n' "${esc_assigns}" | grep 'escape_html')"; escaped_rc=$?
  if [[ "${escaped_rc}" -gt 1 ]]; then
    fail "at least one esc_* is assigned from escape_html" \
      'an esc_*= line calling escape_html' \
      "extraction failed: grep exited ${escaped_rc}"
  elif [[ -z "${esc_escaped_lines}" ]]; then
    fail "at least one esc_* is assigned from escape_html" \
      'an esc_*= line calling escape_html' \
      'no esc_* assignment calls escape_html; the escaper is unwired'
  else
    pass "at least one esc_* is assigned from escape_html"
  fi

  esc_assigned="$(names_on <<<"${esc_assigns}")"
  esc_escaped="$(names_on <<<"${esc_escaped_lines}")"

  # comm needs newline-terminated, sorted input; sed drops the blank line an
  # empty variable would otherwise contribute. Its exit status is checked as
  # well: an unknown comm yields an empty difference, which is exactly the
  # "no differences found" shape the two assertions below key on.
  in_list() { printf '%s\n' "${1}" | sed '/^$/d'; }
  not_escaped="$(comm -23 <(in_list "${esc_assigned}") <(in_list "${esc_escaped}"))"; comm_rc=$?
  unassigned="$(comm -23 <(in_list "${esc_in_heredoc}") <(in_list "${esc_escaped}"))"
  if [[ "${comm_rc}" -ne 0 ]]; then
    fail "comm can compare the esc_* name sets" "exit 0" "exit ${comm_rc}"
  else
    pass "comm can compare the esc_* name sets"
  fi

  # Pin the expected names instead of only asserting that a difference is
  # empty. "not_escaped is empty" also holds when the whole escape block was
  # deleted, and it also holds when a sixth esc_* was added and left unescaped;
  # comparing the sets outright catches both, and catches a silently removed
  # assignment the heredoc still references. The heredoc is the source of
  # truth — it is what actually ships — and expected_esc is the cross-check on
  # it, so a change to one side without the other fails.
  expected_esc='esc_branch
esc_commit_url
esc_short_hash
esc_subject
esc_version_name'

  assert_set_equal "caption heredoc interpolates exactly the known esc_* set" \
    "${expected_esc}" "${esc_in_heredoc}"
  assert_set_equal "tg_body.sh assigns exactly the known esc_* set" \
    "${expected_esc}" "${esc_assigned}"
  assert_set_equal "tg_body.sh assigns every esc_* the heredoc needs" \
    "${esc_in_heredoc}" "${esc_assigned}"
  assert_set_equal "every esc_* assignment calls escape_html" \
    "${esc_in_heredoc}" "${esc_escaped}"

  # The two difference assertions above the pin are kept as well: they name the
  # offending variable, which a set comparison cannot. They are safe to keep
  # now only because the preconditions above guarantee they are comparing two
  # non-empty sets.
  if [[ -n "${not_escaped}" ]]; then
    fail "no esc_* assignment is a raw copy" \
      "each esc_* assigned from escape_html" "raw copies: $(tr '\n' ' ' <<<"${not_escaped}")"
  else
    pass "no esc_* assignment is a raw copy"
  fi

  if [[ -n "${unassigned}" ]]; then
    fail "every esc_* interpolated in the heredoc is assigned from escape_html" \
      "esc_* = escape_html for $(tr '\n' ' ' <<<"${esc_in_heredoc}")" \
      "not escaped: $(tr '\n' ' ' <<<"${unassigned}")"
  else
    pass "every esc_* interpolated in the heredoc is assigned from escape_html"
  fi
fi

# --- 5. upload.sh wiring -----------------------------------------------------
# tg_body.sh reports its own diagnostics on stderr (no jq, API failure). A
# 2>/dev/null on its invocation throws those away and the caption silently
# degrades, which is the failure mode the #113-era guards were added for.
#
# This check already failed closed on "no invocation found", so it was not part
# of the vacuous-assertion bug. It is hardened here anyway for the two ways it
# could still report ok while the thing it looks for is gone:
#   * a *comment* line matching the pattern can stand in for a real invocation
#     that has been removed, and
#   * a 2>/dev/null hidden on the continuation line of a wrapped invocation is
#     invisible to a single-line pattern.
# Comments are therefore filtered out before the existence decision (not after,
# or the -A1 context line would keep the result non-empty on its own), the next
# line is inspected too, and grep's exit status distinguishes "no match" from
# "grep failed". sed does the filtering rather than `grep -v ... || true`
# precisely because it exits 0 on a non-match: the emptiness that matters is
# asserted on the next line, not swallowed by an exit status.
echo
echo "==> upload.sh wiring"
readonly UPLOAD="${SCRIPT_DIR}/upload.sh"
strip_comments() { sed -E '/^[0-9]+[:-]?[[:space:]]*#/d; /^[[:space:]]*#/d'; }

if [[ ! -f "${UPLOAD}" ]]; then
  fail "upload.sh exists" "a readable ${UPLOAD}" 'file not found'
  fail "upload.sh still invokes tg_body.sh" \
    "a \$(... tg_body.sh ...) invocation" 'upload.sh does not exist'
  fail "upload.sh leaves tg_body.sh's stderr attached" \
    'no 2>/dev/null and no 2>&1 on the invocation' 'upload.sh does not exist'
else
  pass "upload.sh exists"

  invoke_hits="$(grep -nE '\$\(.*tg_body\.sh' "${UPLOAD}")"; hits_rc=$?
  invoke_lines="$(printf '%s\n' "${invoke_hits}" | strip_comments)"

  if [[ "${hits_rc}" -gt 1 ]]; then
    fail "upload.sh still invokes tg_body.sh" \
      "a \$(... tg_body.sh ...) invocation" "extraction failed: grep exited ${hits_rc}"
  elif [[ -z "${invoke_lines}" ]]; then
    fail "upload.sh still invokes tg_body.sh" \
      "a \$(... tg_body.sh ...) invocation" 'no invocation found outside a comment'
  else
    pass "upload.sh still invokes tg_body.sh"

    # -A1 so a redirection on the continuation line of a wrapped invocation is
    # still seen; the "--" separator is dropped, comments already were.
    invoke_ctx="$(grep -nA1 -E '\$\(.*tg_body\.sh' "${UPLOAD}" | strip_comments | sed -E '/^--$/d')"
    if grep -qE '2>[[:space:]]*/dev/null|2>&1' <<<"${invoke_ctx}"; then
      fail "upload.sh leaves tg_body.sh's stderr attached" \
        'no 2>/dev/null and no 2>&1 on the invocation' "$(tr '\n' ' ' <<<"${invoke_ctx}")"
    else
      pass "upload.sh leaves tg_body.sh's stderr attached"
    fi
  fi
fi

summary
[[ "${failed}" -eq 0 ]]
