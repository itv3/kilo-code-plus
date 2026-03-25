# Per-Agent Advanced Settings

**Priority:** P2

The CLI backend supports several per-agent configuration options that the extension doesn't expose in its settings UI.

## CLI Agent Config Fields Not Exposed

### Variant (Thinking/Reasoning Default)

- `agent[name].variant` — default thinking/reasoning variant (e.g., `"low"`, `"medium"`, `"high"`)
- Currently, variant selection is only available per-session via the ThinkingSelector in the chat input area
- Not persisted as a default per-agent in settings

### Per-Agent Permissions

- `agent[name].permission` — per-agent tool permission overrides
- Allows different permission profiles per agent (e.g., Code agent can auto-approve edits, Ask agent cannot)
- The Auto Approve tab only sets global permissions, not per-agent

### Agent Visibility

- `agent[name].hidden` — hide agent from the mode switcher without deleting it
- `agent[name].disable` — fully disable an agent
- Currently, the only way to remove an agent is to delete non-native ones

### Agent Mode

- `agent[name].mode` — `"subagent"` / `"primary"` / `"all"`
- Controls whether the agent appears as a primary mode, a subagent, or both
- Not exposed in any UI

### Agent Color

- `agent[name].color` — hex or theme color for agent identification
- No color picker in the agent settings

### Agent Description

- `agent[name].description` — human-readable description
- Displayed as read-only in the current agent list, but not editable

### Agent Options

- `agent[name].options` — arbitrary key-value configuration
- No UI to view or edit these

## Current State in New Extension

The Agents sub-tab exposes:

- Default agent selection
- Per-agent: model override, system prompt, temperature, top-p, max steps
- Remove non-native agents

## Remaining Work

- **Default variant per agent**: Add a variant selector in the per-agent settings that persists to `agent[name].variant` — so users don't have to re-select thinking effort every session
- **Per-agent permissions**: Add a collapsible permission override section per agent, similar to the global Auto Approve tab but scoped to that agent
- **Hidden/disable toggles**: Add toggle switches for hiding and disabling agents
- **Agent mode selector**: Add a dropdown for `"subagent"` / `"primary"` / `"all"` mode
- **Description editing**: Make the description field editable for custom agents
- **Color picker**: Optional — add color selection for visual identification in the mode switcher

## Notes

Not all of these need to be in the initial settings UI. The minimum viable additions are default variant (high user impact) and hidden/disable toggles (users need to manage agent clutter). Per-agent permissions and mode are power-user features that can be deferred.
