---
title: "Concepts"
description: "Understand the building blocks of Gas Town — towns, beads, convoys, rigs, and agents"
---

# {% $markdoc.frontmatter.title %}

Gas Town by Kilo is built around a small set of composable primitives. Understanding these concepts is key to getting the most out of agent orchestration.

## Towns

A **town** is a persistent workspace where agents operate on your code. It maintains:

- **Configuration** — models, merge strategy, review settings, custom instructions
- **Agent state** — which agents exist, what they're working on, their capabilities
- **Work history** — every bead that's been created, worked on, reviewed, and closed
- **Rig connections** — which repositories are connected and how they're configured

Think of a town as a team's workspace — it accumulates institutional knowledge over time. Agents in your town learn from the history of what's been built, reviewed, and merged.

A town can be personal (owned by you) or organizational (shared across a team).

## Beads

A **bead** is the fundamental unit of work. Every task, review, and coordination action is represented as a bead with a lifecycle:

<!-- TODO: React Flow diagram — Bead Lifecycle
  Nodes: open → in_progress → in_review → closed
  With branch for: open → in_progress → failed
  And loop: in_review → open (when review rejects)
  Style: horizontal flow, status-colored nodes (green=closed, blue=in_progress, amber=in_review, red=failed)
-->

| Status | What's happening |
|---|---|
| `open` | Waiting to be picked up by an agent |
| `in_progress` | An agent is actively working on it |
| `in_review` | Work is complete, awaiting review by the refinery |
| `closed` | Successfully completed and merged |
| `failed` | Could not be completed (agent exhausted retries) |

### Bead Types

| Type | Purpose |
|---|---|
| `issue` | A coding task — bug fix, feature, refactor |
| `merge_request` | A review task for the refinery |
| `convoy` | A container for multi-step workflows |
| `escalation` | An issue the agents couldn't resolve — needs human input |
| `message` | Inter-agent communication |

## Convoys

A **convoy** is a multi-bead workflow where tasks can depend on each other. Instead of slinging isolated tasks, convoys let you express complex work as a directed graph.

<!-- TODO: React Flow diagram — Convoy DAG Example
  Nodes: 
    "Explore codebase" (closed) → "Design schema" (closed) → "Implement API" (in_progress)
    "Design schema" (closed) → "Write tests" (open)
    "Implement API" → "Integration tests" (open)
  Style: DAG layout, edges showing dependencies, node colors by status
  Caption: "A convoy executing a 5-bead feature implementation"
-->

When you create a convoy, you define:
- **Tasks** — what needs to be done (each becomes a bead)
- **Dependencies** — which tasks must complete before others can start
- **Feature branch** — a shared branch that convoy work lands on

The reconciler ensures beads are only dispatched when their dependencies are met. This means agents naturally build on each other's work.

### Staged Convoys

Convoys can be **staged** — created but not started immediately. This lets you review the plan before agents begin executing. Un-stage when you're ready to go.

## Rigs

A **rig** connects a repository to your town. Each rig has:

- Its own set of agents (polecats, refinery)
- Branch configuration (default branch, merge target)
- Override settings (model, review mode, merge strategy)

A town can have **multiple rigs** — useful when your project spans several repositories.

## Agents

Agents are the workers in your town. Each has a specialized role:

### Polecats (Coding Agents)

Polecats do the actual software engineering:

- Read and understand your codebase
- Write code changes in isolated git worktrees
- Run tests and commands to verify their work
- Push branches when done

Multiple polecats can work in **parallel** on different beads. The default is 2 per rig, configurable up to 5+.

Each polecat gets its own git worktree — they never conflict with each other or with your local development.

### The Refinery (Review Agent)

The refinery is the quality gate. When a polecat finishes a bead:

1. The refinery reviews the diff
2. Checks for issues, style violations, missing tests
3. Either **approves and merges** or **sends feedback**
4. If feedback is sent, the polecat revises and resubmits

This creates a **micro-adversarial loop** — one agent writes, another critiques, forcing iterative improvement before code lands.

<!-- TODO: React Flow diagram — Micro-Adversarial Loop
  Circular flow: 
    Polecat "writes code" → Refinery "reviews" → (approve) → "merge" 
                                                → (reject) → Polecat "revises" → Refinery "reviews" (loop back)
  Style: circular/orbital layout, animated edges showing the loop, 
  highlight the adversarial tension between write and review
  Caption: "The micro-adversarial loop — code is critiqued before it lands"
-->

### The Mayor (Coordination Agent)

The mayor is your interface to the town:

- Plans convoys from high-level descriptions
- Reports on status and progress
- Triages issues and escalations
- Manages agent configuration
- Answers questions about the codebase and work history

The mayor runs persistently — always available for conversation.

## The Reconciler

The reconciler is the engine that drives the town forward. It runs on every alarm tick (every 5 seconds when work is active) and:

1. **Drains events** — agent completions, status changes, failures
2. **Evaluates rules** — which beads need agents, which convoys are ready to advance
3. **Emits actions** — dispatch an agent, create a review, update convoy progress
4. **Enforces invariants** — no double-dispatch, no orphaned hooks, bounded retries

You don't interact with the reconciler directly — it's the autonomous engine that keeps the town moving.

## The Micro-Adversarial Loop

The most powerful concept in Gas Town is the **micro-adversarial loop**. Rather than trusting a single agent's output, every piece of work goes through an adversarial cycle:

<!-- TODO: React Flow diagram — Full Adversarial Pipeline
  Multi-lane flow showing:
  Lane 1 (Polecat): "Read task" → "Write code" → "Run tests" → "Push branch"
  Lane 2 (Refinery): "Review diff" → decision diamond "Quality?" 
    → Yes: "Merge to target"
    → No: "Send feedback" → (back to Polecat "Revise")
  Lane 3 (Convoy level): Multiple beads flowing through the pipeline in sequence,
    each one building on the merged output of the previous
  
  Caption: "Every bead goes through write → review → revise cycles before landing"
-->

This pattern compounds when combined with **convoys**:

1. Bead 1: Explore the codebase → reviewed → merged to convoy branch
2. Bead 2: Design the schema (builds on bead 1's context) → reviewed → merged
3. Bead 3: Implement the feature (builds on beads 1+2) → reviewed → merged
4. **Landing review**: The full convoy branch is reviewed as a cohesive unit before merging to main

At every stage, work is critiqued and refined. Combined with Kilo's [Code Review](/code-with-ai/gastown/code-review) product, this creates a pipeline where code is reviewed multiple times by different agents with different perspectives before it ever reaches your main branch.

## How It All Fits Together

<!-- TODO: React Flow diagram — Full Town Architecture
  Shows the complete flow:
  User → Mayor (chat) → creates Convoy/Beads
  Reconciler (center, engine) → dispatches Polecats
  Polecat → pushes branch → Refinery reviews → merges
  Container (wrapping polecats) with git worktrees
  Rig connecting to GitHub repo
  
  Style: similar to the Connect talk architecture diagram but simplified for docs
  Caption: "The complete Gas Town architecture"
-->

| Component | Responsibility |
|---|---|
| **You** | Describe work, review PRs, set direction |
| **Mayor** | Plan, coordinate, communicate |
| **Reconciler** | Schedule, dispatch, enforce rules |
| **Polecats** | Write code, run tests, push branches |
| **Refinery** | Review, critique, merge |
| **Container** | Isolated environment with git, tools, runtime |
| **Rig** | Repository connection and configuration |
