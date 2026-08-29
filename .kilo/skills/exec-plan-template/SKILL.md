---
name: exec-plan-template
description: Create detailed ExecPlans following the OpenAI ExecPlan template for feature implementation. Use when the user asks to create a plan, draft an ExecPlan, or plan a new feature.
---

# ExecPlan Template

This skill guides the creation of detailed, self-contained ExecPlans following the [OpenAI ExecPlan template](https://developers.openai.com/cookbook/articles/codex_exec_plans). Plans are living documents that a novice can implement end-to-end.

## Core Principles

- Every plan must be fully self-contained — a novice with only the plan and the working tree can implement the feature end-to-end
- Plans are living documents — revise them as progress is made, discoveries occur, and decisions are finalized
- Use prose-first narrative; avoid tables and checklists outside the `Progress` section
- Define every term of art in plain language; do not refer to external docs
- Anchor with observable outcomes — state what the user can do and what commands to run
- Name files with full repository-relative paths; show exact commands and expected outputs
- Make steps idempotent and safe; include retry or rollback paths for risky operations

## Required Sections

Every ExecPlan must include these sections in order:

### 1. Purpose / Big Picture

A short, action-oriented description of what the feature does and why it exists. State the user-facing outcome, not the technical implementation.

### 2. Progress

A checkbox list with timestamps. Every stopping point must be documented here, even if it requires splitting a partially completed task into two ("done" vs. "remaining"). This section must always reflect the actual current state of the work.

Format:
```
- [x] (YYYY-MM-DD) Description of completed step
- [ ] (YYYY-MM-DD) Description of planned step
```

### 3. Surprises & Discoveries

Document unexpected behaviors, bugs, optimizations, or insights discovered during implementation. Provide concise evidence.

### 4. Decision Log

Record every decision made while working on the plan:
```
- Decision: What was decided
  Rationale: Why this choice was made
  Date/Author: YYYY-MM-DD
```

### 5. Outcomes & Retrospective

Summarize outcomes, gaps, and lessons learned at major milestones or at completion. Compare the result against the original purpose.

### 6. Context and Orientation

Provide enough context for a novice to understand the codebase. Explain:
- Where the feature fits in the architecture
- What existing code it touches
- Key files to read first
- How the layers relate to each other

### 7. Plan of Work

Break the work into milestones. Each milestone must be independently verifiable and incrementally build toward the full feature. The milestones are ordered so that each one produces a working, testable state before the next begins.

For each milestone, describe:
- What it adds or changes
- The observable outcome when complete
- How to verify it works

### 8. Concrete Steps

State the exact commands to run and where to run them (working directory). When a command generates output, show a short expected transcript so the reader can compare.

### 9. Validation and Acceptance

Describe how to start or exercise the system and what to observe. Phrase acceptance as behavior, with specific inputs and outputs.

### 10. Idempotence and Recovery

If steps can be repeated safely, say so. If a step is risky, provide a safe retry or rollback path.

### 11. Artifacts and Notes

Include the most important transcripts, diffs, or snippets as indented examples. Keep them concise and focused on what proves success.

### 12. Interfaces and Dependencies

Be prescriptive. Name the libraries, modules, and services to use and why. Specify the types, traits/interfaces, and function signatures that must exist at the end of the milestone.

## File Location

Place ExecPlans in `.kilo/plans/` with a descriptive filename (e.g., `.kilo/plans/feature-name-plan.md`).

## Formatting Rules

- Use Markdown with clear section headers (`##`)
- Use bullet points for lists
- Use code blocks for commands, expected outputs, and code snippets
- Use full repository-relative paths for all file references
- Show exact commands with expected output transcripts
- Keep the `Progress` section as a checkbox list; use prose elsewhere

## Example Structure

```markdown
# Feature Name ExecPlan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

## Purpose / Big Picture

[What the feature does and why]

## Progress

- [x] (YYYY-MM-DD) Step completed
- [ ] (YYYY-MM-DD) Step planned

## Surprises & Discoveries

[Unexpected findings during implementation]

## Decision Log

- Decision: [What was decided]
  Rationale: [Why]
  Date/Author: YYYY-MM-DD

## Outcomes & Retrospective

[Outcomes and lessons learned]

## Context and Orientation

[Context for a novice]

## Plan of Work

### Milestone 1: [Name]

[Description]

### Milestone 2: [Name]

[Description]

## Concrete Steps

[Commands with expected outputs]

## Validation and Acceptance

[How to verify the feature works]

## Idempotence and Recovery

[Retry and rollback paths]

## Artifacts and Notes

[Key transcripts and snippets]

## Interfaces and Dependencies

[Required classes, functions, and signatures]
```

## Verification

After creating an ExecPlan, verify it has all required sections and follows the formatting rules. The plan should be detailed enough that a novice can implement the feature without prior knowledge of the codebase.
