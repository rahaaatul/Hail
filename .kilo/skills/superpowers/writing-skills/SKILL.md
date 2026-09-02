---
name: writing-skills
description: Use when creating, editing, or verifying skills before deployment. Applies RED-GREEN-REFACTOR: baseline subagent test without skill, write minimal skill addressing specific failures, close loopholes through retesting.
---

# Writing Skills

Skills are TDD applied to process documentation. Untested skills have issues. Always.

## The Iron Law

```
NO SKILL WITHOUT A FAILING TEST FIRST
```

Run baseline scenarios WITHOUT the skill first. Document exact agent choices and rationalizations. Then write the skill.

**No exceptions:** simple additions, documentation updates, "I'll test later", "it's obvious"

## SKILL.md Structure

```yaml
---
name: skill-name-with-hyphens
description: Use when [triggering conditions and symptoms]
---
```

**Description:** "Use when..." plus specific triggers, symptoms, error messages. Never summarize workflow. Under 500 chars.

**Body:** Overview, quick reference table, implementation, common mistakes. Inline code; link files only for heavy reference (>100 lines).

## Skill Discovery Optimization

**Never use `@` file links** — they force-load 200k+ context. Use skill names only.

## Match Form to Failure

| Failure | Right Form | Wrong Form |
|---|---|---|
| Skips rule under pressure | Prohibition + rationalization table | Soft guidance ("prefer...") |
| Wrong output shape | Positive recipe: state what output IS | Prohibition list ("don't X") |
| Omits required element | Structural REQUIRED field | Prose reminders |
| Behavior depends on condition | Conditional on observable predicate | Unconditional + exemptions |

**Never use "Don't X unless..."** — reopens negotiation.

## Discipline Skills: Bulletproofing

| Excuse | Reality |
|---|---|
| "Too simple to test" | Simple breaks. Test takes 30 seconds. |
| "I'll test after" | Tests passing immediately prove nothing. |
| "Tests after achieve same goals" | Tests-after = "what does this do?" Tests-first = "what should this do?" |

Create a red flags list for common rationalizations.

## Testing Workflow

1. **RED**: Baseline scenario without skill. Document choices verbatim.
2. **GREEN**: Minimal skill addressing those exact failures.
3. **REFACTOR**: New rationalization? Add counter. Re-test until bulletproof.

For wording: 5+ fresh-context samples per variant, always include no-guidance control. Variance = wording isn't binding.

## Common Anti-Patterns

- Narrative: "In session X we found..." — not reusable
- Multi-language examples: example-js.js, example-py.py — maintenance burden
- Generic labels: helper1, step3 — use semantic names
- Code in flowcharts: can't copy-paste

## File Organization

```
skills/skill-name/
  SKILL.md           # Required, everything inline unless heavy reference
  supporting-file.*  # Only for tools or 100+ line reference
```

Flat namespace. Name by what you DO: `creating-skills`, not `skill-creation`.

## Deployment Checklist

**RED:**
- [ ] 3+ combined-pressure scenarios
- [ ] Run WITHOUT skill, document baseline
- [ ] Identify rationalization patterns

**GREEN:**
- [ ] Name: letters/numbers/hyphens only
- [ ] YAML frontmatter with description
- [ ] Description: "Use when..." only
- [ ] Keywords for search (errors, symptoms)
- [ ] Address specific baseline failures
- [ ] Form matches failure type
- [ ] Micro-test wording (5+ reps, no-guidance control)
- [ ] Run WITH skill, verify compliance

**REFACTOR:**
- [ ] New rationalizations → explicit counters
- [ ] Red flags list
- [ ] Re-test until bulletproof
