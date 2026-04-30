---
title: "Code Review"
description: "How the refinery agent reviews and merges code"
---

# {% $markdoc.frontmatter.title %}

Every piece of code produced by Gas Town agents goes through automated review before merging. The **refinery** agent is dedicated to critiquing, verifying, and gatekeeping what lands in your codebase.

## The Review Pipeline

When a polecat finishes a bead and pushes its branch, the work enters the review pipeline:

<!-- TODO: React Flow diagram — Review Pipeline
  Linear flow with decision points:
  
  Polecat "pushes branch" 
    → MR bead created (status: open)
    → Refinery dispatched (status: in_progress)
    → Refinery reviews diff
    → Decision: "Quality check"
      → PASS: "Merge" → bead closed ✓
      → FAIL: "Send feedback" → Polecat revises → Re-submit → (loop back to Refinery)
  
  Show retry counter on the loop (max 3 attempts)
  Caption: "The review pipeline — code must pass refinery review before merging"
-->

The refinery evaluates:
- **Correctness** — does the code do what the task asked?
- **Style** — does it follow project conventions?
- **Completeness** — are tests included? Are edge cases handled?
- **Safety** — any security issues, data leaks, or breaking changes?

## Micro-Adversarial Loops

The core insight behind Gas Town's review system is **adversarial iteration**. Rather than one agent producing a final answer, two agents with different objectives improve the output through tension:

<!-- TODO: React Flow diagram — Adversarial Loop Detail
  Circular/orbital layout showing the tension:
  
  Center: "The Code" (evolving artifact)
  
  Orbit 1 (Polecat, blue): 
    Goal: "Ship the feature" → writes code → pushes
    
  Orbit 2 (Refinery, amber):
    Goal: "Protect quality" → reviews → sends feedback OR approves
    
  Connection: feedback arrow from Refinery back to Polecat with example text:
    "Missing error handling in the catch block. The API endpoint should return 
     a structured error response, not swallow the exception."
  
  Polecat then: "Revises with feedback" → pushes again → Refinery re-reviews
  
  Style: animated orbital, showing the push-pull dynamic
  Caption: "Adversarial tension between 'ship it' and 'make it better'"
-->

This pattern is fundamentally different from having a single agent self-review:
- Self-review has a **confirmation bias** — the same "mind" that wrote the code evaluates it
- Adversarial review creates **genuine tension** — the refinery has different priorities than the polecat
- Each revision cycle **measurably improves** the output because feedback is specific and actionable

### The Loop in Practice

A typical bead goes through 1-2 revision cycles:

| Cycle | What happens |
|---|---|
| **Write** | Polecat reads the task, writes code, runs tests, pushes |
| **Review 1** | Refinery finds 2 issues: missing test case, inconsistent naming |
| **Revise 1** | Polecat adds the test, fixes naming, pushes again |
| **Review 2** | Refinery approves — code meets quality bar |
| **Merge** | Code lands on target branch |

After 3 failed revision cycles, the bead escalates rather than looping forever.

## Merge Strategies

Gas Town supports two merge strategies, configurable per-town or per-rig:

### Direct Merge

The refinery merges directly to the target branch (convoy feature branch or main) without creating a GitHub PR. This is faster but gives you less visibility into individual merges.

**Best for:** trusted agent output, internal projects, rapid iteration.

### Pull Request Mode

The refinery creates a GitHub PR for each merge. The PR includes:
- The diff
- Review comments from the refinery
- Status checks from CI

You can configure whether PRs auto-merge after refinery approval or require human approval.

**Best for:** production codebases, team environments, audit trails.

## Convoy-Level Review

Convoys add an additional review layer beyond per-bead review:

<!-- TODO: React Flow diagram — Convoy Review Layers
  Three columns showing review at different scopes:
  
  Column 1 "Per-Bead Review":
    Bead 1 → Refinery ✓
    Bead 2 → Refinery ✓  
    Bead 3 → Refinery ✓ (with one revision cycle shown)
    
  Column 2 "Landing Review":
    Combined diff (all 3 beads) → Refinery reviews holistically
    "Does the combined work make sense as a feature?"
    
  Column 3 "Human Review" (optional):
    Final PR to main → Your team reviews
    
  Caption: "Three layers of review — individual, combined, and human"
-->

| Review Layer | What's checked | Who reviews |
|---|---|---|
| Per-bead | Individual contribution quality | Refinery agent |
| Landing | Combined feature coherence | Refinery agent |
| Human (optional) | Business logic, architecture | Your team |

## Combining with Kilo Code Review

Gas Town's refinery works independently, but combining it with [Kilo Code Review](/code-with-ai/platforms/cloud-agent) creates an even stronger pipeline:

1. **Agent writes** → agent-level refinery reviews (fast, automated)
2. **Code lands as PR** → Kilo Code Review provides human-readable review (deeper, contextual)
3. **Human approves** → code ships

This gives you automated adversarial review for speed **plus** AI-assisted human review for judgment — the best of both approaches.

## Review Configuration

Customize the refinery's behavior in **Town Settings** → **Review**:

| Setting | Options | Default |
|---|---|---|
| `review_mode` | `always` / `never` / `pr_only` | `always` |
| `merge_strategy` | `direct` / `pr` | `direct` |
| `auto_merge` | `true` / `false` | `true` |
| `review_gates` | Strictness level (1-5) | 3 |
| `max_review_cycles` | How many revision attempts | 3 |

### Review Mode

- **`always`** — every bead goes through refinery review (recommended)
- **`never`** — skip review, merge directly on polecat completion (fast but risky)
- **`pr_only`** — only review work that creates a PR

### Review Gates

Higher gate levels make the refinery stricter:
- **Level 1** — basic sanity (compiles, doesn't break tests)
- **Level 3** — standard (style, tests, correctness) — default
- **Level 5** — strict (architecture review, performance, security)

## Handling Review Feedback

When the refinery rejects a submission, it provides specific, actionable feedback. The polecat receives this feedback and revises accordingly. You can see the feedback exchange in the bead's event history.

If a bead fails review 3 times, it transitions to `failed` and creates an **escalation** for human attention. This prevents infinite loops while ensuring difficult code doesn't slip through without proper quality.
