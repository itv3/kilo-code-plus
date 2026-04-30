---
title: "Quick Start"
description: "Get your first Gas Town running in minutes"
---

# {% $markdoc.frontmatter.title %}

This guide walks you through creating your first town, connecting a repository, and watching agents work on real code.

## Prerequisites

- A [Kilo account](https://app.kilo.ai) (free tier works)
- A GitHub repository you want agents to work on
- A GitHub Personal Access Token (recommended)

## 1. Create a Town

From the Kilo dashboard, click **New Town**. Give it a name — this is just for your reference.

<!-- TODO: Screenshot — "New Town" button and creation dialog -->

## 2. Connect a Repository

Add a **rig** to your town. A rig is a connection to a specific repository.

1. Click **Add Rig**
2. Select your GitHub repository (or paste the URL)
3. Choose the default branch (usually `main`)
4. Click **Connect**

Gastown uses the [Kilo GitHub App](https://github.com/apps/kilo-code) to access your repository. You'll be prompted to install it if you haven't already.

<!-- TODO: Screenshot — rig creation flow showing repo selection -->

## 3. Add a GitHub Personal Access Token

{% callout type="tip" title="Recommended" %}
Adding a GitHub PAT means all commits, branches, and PRs created by your town's agents will appear as **you** — not a bot account. It also enables agents to use `gh` CLI commands (creating issues, commenting on PRs, etc.) on your behalf.
{% /callout %}

1. Go to **Town Settings** → **Git & Authentication**
2. Paste your GitHub Personal Access Token
3. The token needs `repo` scope (and `workflow` if your repo uses GitHub Actions)

Without a PAT, agents use the GitHub App installation token — functional but shows up as a bot in your git history.

## 4. Sling Your First Task

Now let's give the agents something to do:

1. Click **Sling Work** (or ask the Mayor)
2. Describe a simple task, e.g.: *"Add a CONTRIBUTING.md file with basic setup instructions"*
3. Click **Sling**

<!-- TODO: Screenshot — Sling Work dialog with a sample task -->

## 5. Watch Agents Work

The reconciler assigns your task to an available polecat agent. You'll see:

1. A **bead** appear in the beads list with status `open`
2. The bead transitions to `in_progress` as a polecat picks it up
3. The agent reads your code, makes changes, and pushes a branch
4. The bead moves to `in_review` as the refinery checks the work
5. The refinery merges (or creates a PR depending on your settings)
6. The bead reaches `closed`

<!-- TODO: Screenshot — bead lifecycle showing the transitions in real-time -->

The whole cycle typically takes 2-10 minutes depending on complexity and the model you're using.

## 6. Talk to the Mayor

Click the **Mayor** chat to interact with your town's coordinator. Try:

- *"What's the status of the town?"*
- *"Create a convoy to add unit tests for the auth module"*
- *"What settings should I change for faster reviews?"*

The Mayor is always running — it's your primary interface for managing the town conversationally.

<!-- TODO: Screenshot — Mayor chat with a greeting and status response -->

## What's Next?

- [Concepts](/code-with-ai/gastown/concepts) — Understand the building blocks in depth
- [Sling Work](/code-with-ai/gastown/sling-work) — Learn about convoys and multi-step workflows
- [Settings](/code-with-ai/gastown/settings) — Configure models, merge strategies, and agent behavior
