---
title: "Gas Town by Kilo"
description: "Autonomous AI agent orchestration for your codebase"
---

# {% $markdoc.frontmatter.title %}

Gastown by Kilo is an autonomous agent orchestration platform that manages teams of AI agents working on your codebase. Built on [Gastown](https://gastown.dev) — the open protocol for agent orchestration — Kilo's implementation coordinates coding agents, a code review agent, and a conversational coordinator to ship features, fix bugs, and maintain your projects with minimal human intervention.

You describe the work. Agents figure out how to do it, write the code, review each other's output, and land clean PRs — while you stay in control of what ships.

<!-- TODO: Screenshot — town overview page showing active agents, beads in progress, and a recent merge -->

## What Makes Gastown Different

Unlike single-agent coding tools that handle one task at a time, Gastown orchestrates **multiple specialized agents** working in parallel:

| Agent | Role | What it does |
|---|---|---|
| **Polecats** | Coding | Write code, run tests, push branches. Multiple can work simultaneously on different tasks. |
| **Refinery** | Code Review | Reviews polecat output, runs quality checks, merges approved work. |
| **Mayor** | Coordination | Your conversational interface. Plans work, answers questions, triages issues, keeps things moving. |

These agents operate within a **town** — a persistent workspace connected to your repository. The town maintains state across sessions: work history, agent configuration, and institutional knowledge about your codebase.

<!-- TODO: Screenshot — the mayor chat interface showing a conversation about planning work -->

## How It Works

### 1. Create a town

Connect a GitHub repository to a new town. Gastown provisions a dedicated environment with agents ready to work.

### 2. Sling work

Describe what needs to be done — a bug to fix, a feature to build, a refactor to execute. You can sling a single task or a **convoy** (a multi-step plan where tasks depend on each other).

<!-- TODO: Screenshot — the "Sling Work" dialog or the convoy creation UI -->

### 3. Agents pick it up

The reconciler assigns open work to available polecats. Each agent gets its own git worktree, writes code, runs commands, and pushes a branch when done.

### 4. Code gets reviewed

Completed work flows to the refinery for automated code review. Depending on your merge strategy, the refinery either merges directly or creates a PR for your approval.

### 5. You stay in control

Monitor progress from the town dashboard. Chat with the mayor. Review PRs. Adjust priorities. Intervene when agents need guidance. Everything the agents do is visible — branches, commits, review comments, and decision reasoning.

<!-- TODO: Screenshot — beads page showing a mix of closed, in-progress, and open beads -->

## Core Concepts

| Concept | What it is |
|---|---|
| **Town** | A persistent workspace connecting your repo to a team of agents. Maintains configuration, history, and state. |
| **Bead** | A unit of work — an issue to fix, a task to complete, a review to perform. Beads have a lifecycle: open, in progress, in review, closed. |
| **Convoy** | A multi-bead workflow. Beads within a convoy can depend on each other, forming a DAG that agents execute in order. |
| **Rig** | A repository connection within a town. Each rig has its own branch configuration, agents, and settings. A town can have multiple rigs. |
| **Mayor** | The coordination agent. Your primary interface for interacting with the town through natural language. |
| **Polecat** | A coding agent. Works on individual beads — reads code, makes changes, runs tests, pushes branches. |
| **Refinery** | The review agent. Checks polecat output for quality, performs merges, and gates what lands in your codebase. |

For a deeper dive, see [Concepts](/code-with-ai/gastown/concepts).

## The Mayor — Your Interface to the Town

The mayor is how you interact with Gastown conversationally. Think of it as a technical lead that knows your codebase, tracks what all agents are doing, and can take action on your behalf.

You can ask the mayor to:

- Plan and create convoys from high-level descriptions
- Check on the status of in-progress work
- Investigate failures and stuck agents
- Update town settings and configuration
- Triage incoming issues or bug reports

The mayor runs persistently in your town — it's always available, even when no coding agents are active.

<!-- TODO: Screenshot — mayor conversation showing it creating a convoy from a user request -->

## What You Can Build With Gastown

- **Feature development** — Describe a feature, get back a PR with tests
- **Bug fixes** — Point agents at an issue, they investigate and fix it
- **Refactoring** — Large-scale codebase changes executed methodically across convoys
- **Maintenance** — Dependency updates, lint fixes, documentation generation
- **Code review** — Automated first-pass review on all agent and human PRs

## Getting Started

- [Quick Start](/code-with-ai/gastown/quick-start) — Create your first town and see agents work in under 5 minutes
- [Concepts](/code-with-ai/gastown/concepts) — Detailed explanation of towns, beads, convoys, and the agent lifecycle
- [The Mayor](/code-with-ai/gastown/mayor) — How to interact with the coordination agent effectively

## Guides

- [Sling Work](/code-with-ai/gastown/sling-work) — Creating tasks, convoys, and staged workflows
- [Code Review](/code-with-ai/gastown/code-review) — Merge strategies, review gates, and the refinery pipeline
- [Settings](/code-with-ai/gastown/settings) — Model configuration, agent limits, custom instructions, and environment variables
- [Troubleshooting](/code-with-ai/gastown/troubleshooting) — Common issues and how to resolve them
