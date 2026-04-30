---
title: "Gastown"
description: "Autonomous AI agent orchestration for your codebase"
---

# {% $markdoc.frontmatter.title %}

Gastown is an autonomous agent orchestration platform that manages multiple AI agents working on your codebase. It coordinates polecats (coding agents), a refinery (code review agent), and a mayor (coordination agent) to handle tasks, code reviews, and merges — all without constant human oversight.

## Core Concepts

- **Towns** — A workspace that connects to your repository and manages agents working on it
- **Beads** — Units of work (issues, tasks, reviews) that agents pick up and complete
- **Convoys** — Multi-step workflows that chain beads together in a dependency graph
- **Rigs** — Repository connections within a town, each with their own agents and configuration
- **The Mayor** — Your primary interface to the town; a conversational agent that coordinates work and answers questions

## How It Works

1. **Create a town** and connect it to your repository
2. **Sling work** — describe what you need done, and agents autonomously pick it up
3. **Agents collaborate** — polecats write code, the refinery reviews and merges, the mayor coordinates
4. **You stay in control** — review PRs, adjust settings, intervene when needed

## Getting Started

- [Quick Start](/code-with-ai/gastown/quick-start) — Get your first town running in minutes
- [Concepts](/code-with-ai/gastown/concepts) — Understand towns, beads, convoys, and agents
- [The Mayor](/code-with-ai/gastown/mayor) — How to interact with your town's coordinator

## Guides

- [Sling Work](/code-with-ai/gastown/sling-work) — Creating tasks and convoys for agents
- [Code Review](/code-with-ai/gastown/code-review) — How the refinery reviews and merges code
- [Settings](/code-with-ai/gastown/settings) — Configure models, merge strategies, and agent behavior
- [Troubleshooting](/code-with-ai/gastown/troubleshooting) — Common issues and how to resolve them
