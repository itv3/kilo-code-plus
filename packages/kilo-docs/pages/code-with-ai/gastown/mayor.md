---
title: "The Mayor"
description: "How to interact with your town's coordination agent"
---

# {% $markdoc.frontmatter.title %}

The Mayor is your primary interface to a Gas Town. It's a persistent conversational agent that coordinates work, answers questions, and takes action on your behalf.

## What the Mayor Does

The Mayor operates as a technical lead that:

- **Plans work** — converts high-level descriptions into convoys and beads
- **Reports status** — knows what every agent is doing, what's stuck, what's shipped
- **Triages issues** — investigates failures, stuck agents, and escalations
- **Configures the town** — updates settings, manages rigs, adjusts agent behavior
- **Answers questions** — about the codebase, work history, and town state

Unlike polecats (which spin up to work on specific beads), the Mayor runs **persistently**. It's always available, even when no coding agents are active.

{% browserFrame url="app.kilo.ai/gastown/town" caption="The Mayor chat — always available for coordination and questions" %}
{% image src="/docs/img/gastown/gt-town-overview.png" alt="Gas Town Mayor chat interface" /%}
{% /browserFrame %}

## Talking to the Mayor

Open the Mayor panel from your town dashboard. The conversation is persistent — the Mayor remembers context from previous messages within the same session.

### Planning Work

Ask the Mayor to create work for the town:

> *"Create a convoy to add authentication to the API. We need JWT token generation, middleware for protected routes, and integration tests."*

The Mayor will:
1. Break this into individual beads with dependencies
2. Propose a convoy plan for your review
3. Create the convoy (staged by default, so you can review before agents start)

### Checking Status

> *"What's everyone working on?"*
> *"Is anything stuck?"*
> *"How did the last convoy go?"*

The Mayor has full visibility into agent state, bead progress, and recent history.

### Investigating Problems

> *"The auth refactor bead has been in progress for 20 minutes — what's happening?"*
> *"Why did the refinery reject the last review?"*

The Mayor can inspect agent status messages, review feedback, and container logs to diagnose issues.

### Updating Configuration

> *"Switch the model to Claude Opus for this town"*
> *"Set max polecats to 4"*
> *"Add a custom instruction: always use TypeScript strict mode"*

The Mayor can modify town settings on your behalf through natural language.

## Mayor Tools

The Mayor has access to specialized tools for town management:

| Tool | What it does |
|---|---|
| `gt_sling` | Create a bead or convoy |
| `gt_convoy_close` | Force-close a stuck convoy |
| `gt_agent_reset` | Reset a stuck agent |
| `gt_bead_delete` | Delete beads (single or bulk) |
| `gt_report_bug` | File a bug report about town behavior |
| `gt_done` | Mark work as complete |

These tools are used automatically when you make requests — you don't need to invoke them directly.

## Tips for Effective Communication

### Be specific about scope

Instead of: *"Fix the bugs"*

Try: *"Fix the TypeScript type errors in src/auth/. There are 3 reported in the CI output."*

### Use convoys for complex work

Instead of: *"Add a user dashboard with charts, settings, and notifications"*

Try: *"Create a staged convoy for the user dashboard. Break it into: 1) dashboard layout and navigation, 2) chart components with mock data, 3) settings page, 4) notification system. Each should build on the previous."*

### Let the Mayor triage

When something goes wrong, ask the Mayor before investigating yourself:

> *"Bead abc123 has been stuck for 30 minutes — can you investigate?"*

The Mayor can often diagnose and resolve issues (reset agents, close stuck convoys, re-dispatch work) without you needing to dig into the admin panel.

## Mayor Limitations

- The Mayor coordinates but doesn't write code itself — that's what polecats are for
- It can only see what's happening inside your town, not external systems
- Complex multi-repo orchestration may need manual coordination between towns
- The Mayor's context window is bounded — very long conversations may lose early context
