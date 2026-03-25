# Experiments & Feature Flags Parity

**Priority:** P3

The legacy extension had a set of experimental features controlled by feature flags. The new extension has its own experimental features but some legacy experiments may still be relevant.

## Legacy Experiments Not in New Extension

### Power Steering

- Reinforced mode-specific behaviour in the system prompt
- Made the agent more strictly follow its mode's role definition
- Off by default

### Multi-File Apply Diff

- Allowed `apply_diff` to modify multiple files in a single tool call
- Reduced round-trips for cross-file refactoring

### Prevent Focus Disruption

- Prevented VS Code from stealing focus during agent operations
- Important for users working in other files while the agent runs

### Image Generation

- Enabled a `generate_image` tool for the agent
- Off by default

### Multiple Native Tool Calls

- Allowed the agent to make multiple native function calls in a single response
- Reduced latency for multi-step operations

### Custom Tools

- Allowed defining custom tool schemas for the agent
- Power-user feature for extending the tool set

## Current Extension Experiments

The extension already has:

- Share mode (manual/auto/disabled)
- Formatter toggle
- LSP toggle
- Disable paste summary
- Batch tool
- Codebase search
- Continue on deny
- MCP timeout
- Per-tool enable/disable toggles

## Remaining Work

- **Audit legacy experiments**: Determine which legacy experiments are relevant in the CLI architecture:
  - Power steering → Does the CLI have mode reinforcement? Is it configurable?
  - Prevent focus disruption → Still relevant for VS Code extension, may need extension-side implementation
  - Custom tools → CLI supports plugins — determine overlap
- **Prevent focus disruption**: This is extension-side (VS Code API) — implement a toggle to prevent `window.showTextDocument` and similar focus-stealing calls during agent execution
- **Review CLI experiments**: Check if the CLI has its own experimental features that should be exposed in the extension's Experimental tab

## Notes

Some legacy experiments became default behaviour or were removed. Focus on experiments that improve the user experience in the VS Code context rather than porting every legacy flag.
