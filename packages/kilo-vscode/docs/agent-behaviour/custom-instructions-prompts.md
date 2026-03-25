# Custom Instructions & Prompts Tab

**Priority:** P1

The Prompts tab in Settings shows "Not implemented". The legacy extension had extensive custom instructions support that users rely on to shape agent behaviour.

## Legacy Features

### Global Custom Instructions

- Free-text field stored in global state (`customInstructions`)
- Injected into every system prompt under "Global Instructions"
- Accessible from the settings panel

### Per-Mode Custom Instructions

- Each mode (Code, Architect, Ask, Debug, etc.) could have its own `customInstructions` override
- Stored in `customModePrompts` — a record of mode slug to prompt components
- Allowed overriding `roleDefinition`, `whenToUse`, `customInstructions`, and `description` per mode

### Custom Support Prompts

- Overridable templates for specific agent actions:
  - `ENHANCE` — prompt enhancement
  - `CONDENSE` — context condensation/summarization
  - `EXPLAIN` — explain code
  - `FIX` — fix code
  - `IMPROVE` — improve code
  - `ADD_TO_CONTEXT` — add to context
  - `TERMINAL_*` — terminal command generation
  - `NEW_TASK` — new task from selection
  - `COMMIT_MESSAGE` — commit message generation
- Each could be customized with user-provided templates

### Language Preference

- Setting to inject language preference into system prompt (e.g., "speak and think in Japanese")
- Integrated with i18n

## Current State in New Extension

- The CLI backend supports per-agent `prompt` (system prompt) and `temperature` — these are already exposed in the Agents sub-tab of AgentBehaviour settings
- The Prompts tab exists but shows a placeholder
- No UI for global custom instructions that apply across all agents
- No UI for custom support prompts (enhance, condense, explain, etc.)
- Language preference is handled by the Language tab (already implemented)

## Remaining Work

- **Implement the Prompts tab** with sections for:
  - Global custom instructions text area (applies to all agents/sessions)
  - Per-support-action prompt templates (if CLI supports overridable action prompts)
- **Determine CLI mapping**: The CLI's per-agent `prompt` field covers per-agent system prompts. Determine whether the CLI has or needs:
  - A global instructions field (separate from per-agent prompts)
  - Overridable action-specific prompts (enhance, condense, commit message, etc.)
- **Migration path**: The settings migration (settings-migration.md) should map legacy `customInstructions` and `customModePrompts` to CLI equivalents — likely per-agent `prompt` fields and/or a global instructions config key
- **Rule files as alternative**: Document that `.kilocode/rules/` and `AGENTS.md` serve as the filesystem-based equivalent of global custom instructions in the new architecture

## Notes

The CLI's rule files (`.kilocode/rules/`, `AGENTS.md`) partially replace the need for a global custom instructions text field. The Rules sub-tab already exposes instruction file paths. Consider whether a dedicated text field is still needed or if the Prompts tab should focus on action-specific prompt templates that don't have a filesystem equivalent.
