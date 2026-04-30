---
title: "Sling Work"
description: "Creating tasks and convoys for agents to work on"
---

# {% $markdoc.frontmatter.title %}

"Slinging work" is how you give agents something to do. You can sling a single task (a bead) or a structured multi-step plan (a convoy).

## Single Tasks

The simplest way to use Gas Town — describe what needs to be done, and an agent picks it up:

1. Click **Sling Work** in the town header
2. Write a description: *"Fix the 404 error on the /settings page — the route is missing from the router config"*
3. Click **Sling**

The reconciler assigns the bead to an available polecat. The agent reads the relevant code, makes the fix, runs any tests, and pushes a branch.

<!-- TODO: Screenshot — Sling Work dialog with a single task -->

### Writing Good Task Descriptions

The quality of agent output directly correlates with the clarity of your description:

| Approach | Example |
|---|---|
| **Vague** (avoid) | "Fix the auth" |
| **Specific** (better) | "Fix the JWT token expiration — tokens should last 24h, not 1h. The constant is in `src/auth/config.ts`" |
| **Contextual** (best) | "Fix #142: users are getting logged out after 1 hour. The issue is the JWT expiration in `src/auth/config.ts` is set to 3600 (1h) but should be 86400 (24h). Add a test to verify." |

Include:
- **What** needs to change
- **Where** in the codebase (file paths if you know them)
- **Why** it's needed (link to issues, error messages)
- **How to verify** (tests to run, behavior to check)

## Convoys

Convoys are where Gas Town really shines. Instead of one agent doing everything in one pass, convoys break complex work into stages where each builds on the last — with adversarial review at every step.

### Why Convoys?

Single-pass agent output has a quality ceiling. The longer an agent works on one task, the more likely it is to accumulate compounding errors. Convoys solve this by:

1. **Decomposing** complex work into focused, reviewable chunks
2. **Sequencing** so later steps build on reviewed, merged code
3. **Reviewing** each chunk independently before it becomes the foundation for the next step
4. **Containing failures** — if step 3 fails, steps 1 and 2 are already safely merged

<!-- TODO: React Flow diagram — Convoy vs Single-Pass Comparison
  Two parallel flows:
  
  Top (Single-Pass): 
    "Big task" → Polecat works 30min → "Large PR (800 lines)" → Review struggles → Bugs land
    
  Bottom (Convoy):
    "Explore" → review ✓ → "Design" → review ✓ → "Implement" → review ✓ → "Test" → review ✓ → Clean merged result
    Each step: small PR, easy review, bugs caught early
    
  Caption: "Convoys produce higher quality output through iterative adversarial review"
-->

### Creating a Convoy

**Via the UI:**
1. Click **Sling Work** → **Convoy**
2. Add tasks in order
3. Define dependencies (which tasks block others)
4. Choose **staged** (review plan first) or **immediate** (start right away)

**Via the Mayor:**
> *"Create a convoy to migrate the database from PostgreSQL to MySQL. Steps: 1) audit current schema and queries, 2) design the new schema with migration plan, 3) implement the migration scripts, 4) update the application layer, 5) add integration tests"*

The Mayor converts this into a convoy with proper dependencies.

<!-- TODO: Screenshot — Convoy creation UI showing task list with dependency arrows -->

### Convoy Execution

Once started, the reconciler manages the convoy:

<!-- TODO: React Flow diagram — Convoy Execution Flow
  Animated stage-by-stage flow:
  
  Stage 1: Bead "Audit schema" dispatched to Polecat-1
    → Polecat works → pushes branch → Refinery reviews → MERGE to convoy branch
  
  Stage 2: Bead "Design migration" dispatched to Polecat-2 (starts from convoy branch)  
    → Polecat works (has context from stage 1) → pushes → Refinery reviews → MERGE
  
  Stage 3: Bead "Implement scripts" dispatched (starts from convoy branch with stages 1+2)
    → works → pushes → reviews → MERGE
  
  Final: "Landing Review" — full convoy branch reviewed as cohesive unit → MERGE to main
  
  Caption: "Each stage builds on merged, reviewed work from previous stages"
-->

Key behaviors:
- Each polecat starts from the **convoy feature branch**, which accumulates all previously merged work
- Beads only dispatch when their **dependencies are satisfied** (upstream beads closed)
- The refinery reviews each sub-PR against the convoy branch
- Once all beads close, a **landing review** checks the full combined diff before merging to main

### The Adversarial Advantage

The convoy pattern creates **layered adversarial review**:

1. **Per-bead review** — refinery critiques each individual contribution
2. **Context accumulation** — each agent builds on verified, reviewed code
3. **Landing review** — the complete feature is reviewed holistically
4. **Combined with Kilo Code Review** — if configured, human reviewers see the final PR too

This means code goes through **3-4 review passes** before landing in your main branch. Bugs get caught at the smallest possible scope where they're cheapest to fix.

### Staged Convoys

By default, convoys are created **staged** — the plan exists but agents don't start until you un-stage it. This lets you:

- Review the task breakdown before execution
- Adjust descriptions, add context, reorder
- Ensure the plan makes sense before burning compute

Un-stage via the convoy detail page or ask the Mayor: *"Start the database migration convoy"*

## Assigning Priority

Beads have priority levels: `low`, `medium` (default), `high`, `critical`.

Higher priority beads are dispatched first when multiple beads are waiting for agents. Set priority:
- In the Sling Work dialog
- Via the Mayor: *"Make the auth fix high priority"*
- By editing the bead after creation

## Watching Progress

### Beads Page

The beads list shows all work in your town with real-time status updates. Filter by:
- Status (open, in progress, in review, closed, failed)
- Type (issue, merge_request, convoy)
- Rig (if you have multiple repos)

<!-- TODO: Screenshot — Beads page with filter controls and mixed status beads -->

### Town Overview

The town overview shows a high-level summary:
- Active agents and what they're working on
- Recent completions
- Pending work queue

### Real-Time Events

The event timeline shows every state transition as it happens — bead dispatched, review submitted, merge completed. Useful for understanding the flow in real-time.
