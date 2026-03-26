---
title: "Orchestrator Mode"
description: "Orchestrator mode is no longer needed — all agents now support subagents"
---

# Orchestrator Mode

{% callout type="info" title="Orchestrator mode is no longer needed" %}
In the VSCode extension and CLI, **every agent can now delegate to subagents automatically**. You no longer need a dedicated orchestrator — just pick the agent for your task (e.g. Code, Plan, Debug) and it will coordinate subagents when helpful. The orchestrator agent still exists for backward compatibility, but there is no advantage to using it directly.
{% /callout %}

## What Changed

Previously, orchestrator mode was the only way to break complex tasks into subtasks. You had to explicitly switch to orchestrator mode, which would then delegate work to other modes like Code or Architect.

Now, **subagent support is built into every agent**. When any agent encounters a task that would benefit from delegation — like exploring a codebase, running a parallel search, or handling a subtask in isolation — it can launch a subagent directly using the `task` tool. There's no need to switch agents first.

## What You Should Do

- **Just pick the right agent for your task.** Use Code for implementation, Plan for architecture, Debug for troubleshooting. Each will orchestrate subagents where it makes sense.
- **Add custom subagents** if you want specialized delegation behavior. See [Custom Subagents](/docs/customize/custom-subagents) for details.
- **Stop switching to orchestrator mode** before complex tasks. Your current agent already has that capability.

## How Subagents Work

1. The agent analyzes a complex task and decides a subtask would benefit from isolation.
2. It launches a subagent session using the `task` tool (e.g., `general` for autonomous work, `explore` for codebase research).
3. The subagent runs in its own isolated context — separate conversation history, no shared state.
4. When done, the subagent returns a summary to the parent agent, which continues its work.

Agents can launch multiple subagent sessions concurrently for parallel work.

{% callout type="tip" title="Keep Tasks Focused" %}
Use subtasks to maintain clarity. If a request significantly shifts focus or requires different expertise, the agent can create a subtask rather than overloading its current context.
{% /callout %}

{% callout type="info" title="VSCode (Legacy)" collapsed=true %}
In the legacy extension, orchestrator mode uses two dedicated tools:

1. [`new_task`](/docs/automate/tools/new-task) — Creates a subtask with context passed via the `message` parameter and a mode specified via `mode` (e.g., `code`, `architect`, `debug`).
2. [`attempt_completion`](/docs/automate/tools/attempt-completion) — Signals subtask completion and passes a summary back to the parent via the `result` parameter.

{% youtube url="https://www.youtube.com/watch?v=20MmJNeOODo" caption="Orchestrator Mode in the legacy extension" /%}
{% /callout %}
