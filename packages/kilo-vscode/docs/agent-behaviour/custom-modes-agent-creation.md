# Custom Modes / Agent Creation UI

**Priority:** P2

The legacy extension had a full custom mode creation system. The new extension can only view existing agents and remove non-native ones — there is no UI to create or fully edit custom agents.

## Legacy Features

### Custom Mode Definition

Each custom mode was a `ModeConfig` with:

- `slug` — unique identifier
- `name` — display name
- `roleDefinition` — the system prompt role definition (required)
- `whenToUse` — description for orchestrator mode selection
- `description` — short human-readable description
- `customInstructions` — mode-specific additional instructions
- `groups` — array of tool group entries with optional file restrictions (e.g., `["edit", { fileRegex: "\\.md$", description: "Markdown files only" }]`)
- `source` — `"global"`, `"project"`, or `"organization"`
- `iconName` — codicon icon name

### Storage

- Global state (`customModes`)
- Project-level `.kilocodemodes` file in workspace root
- Global `custom_modes.yaml` file
- Modes could be imported from the Marketplace

### Tool Group Restrictions

- Per-mode tool group selection (read, edit, browser, command, mcp)
- File regex restrictions per tool group (e.g., architect mode could only edit `.md` files)

## Current State in New Extension

- Agents sub-tab shows agent name, description, and per-agent settings (model, prompt, temperature, top-p, max steps)
- Non-native agents can be removed
- No UI to create a new agent/mode
- No UI to edit tool groups or file restrictions
- No UI to set `whenToUse` or orchestrator hints
- The CLI supports custom agents via config — the extension could expose creation through config writes

## Remaining Work

- **Create Agent dialog/form**: UI to define a new custom agent with:
  - Name, slug (auto-generated from name)
  - Role definition (system prompt)
  - Description and `whenToUse` text
  - Tool group selection with optional file regex restrictions
  - Model override (already supported per-agent)
  - Temperature and other generation parameters
- **Edit Agent**: Allow editing all fields of existing custom agents (not just model/prompt/temperature)
- **Agent source management**: Show where each agent is defined (global config, project config, CLI built-in)
- **Import from Marketplace**: Connect to the Marketplace feature for installing community-created modes
- **CLI config writes**: Use the config update endpoint to persist new agents to `opencode.json`
- **Per-agent permissions**: Expose CLI's `agent[name].permission` for per-agent tool permission overrides
- **Agent visibility**: Expose CLI's `agent[name].hidden` and `agent[name].disable` toggles

## Notes

The CLI's agent model is more flexible than the legacy extension's mode model — it supports `mode` (subagent/primary/all), `variant`, `color`, and `options` fields that have no legacy equivalent. The creation UI should expose the full CLI agent config, not just legacy mode fields.
