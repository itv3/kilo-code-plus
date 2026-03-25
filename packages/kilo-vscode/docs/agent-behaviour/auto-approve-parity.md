# Auto-Approve Feature Parity

**Priority:** P1

The new extension has a functional Auto Approve tab with per-tool permission levels (Allow/Ask/Deny) and granular path/command exceptions. However, several legacy auto-approve features are missing.

## Legacy Features Not Yet Covered

### YOLO Mode

- Master toggle that bypasses ALL approval checks
- Also removed `ask_followup_question` from available tools so the agent doesn't pause to ask questions
- Separate from individual per-tool toggles — a single "approve everything" switch

### YOLO Gatekeeper

- Kilo-specific AI safety gatekeeper for YOLO mode
- Used a separate API configuration (`yoloGatekeeperApiConfigId`) to call a second model
- The gatekeeper model evaluated each action and could approve/deny it
- Provided a safety net for fully autonomous operation

### Max Requests per Task

- `allowedMaxRequests` — limit on number of API requests per task
- When reached, paused and asked user for approval to continue
- Prevented runaway agent loops

### Max Cost per Task

- `allowedMaxCost` — limit on total API call cost per task
- When exceeded, paused and asked user for approval
- Budgeting control for expensive operations

### Command Timeout

- `commandExecutionTimeout` — timeout for shell command execution (0-600 seconds)
- `commandTimeoutAllowlist` — commands exempt from timeout
- Prevented long-running commands from blocking the agent

### Toggle Auto-Approve Keybinding

- `Cmd+Alt+A` / `Ctrl+Alt+A` keyboard shortcut to toggle auto-approve on/off
- Quick access without opening settings

## Current State in New Extension

- Auto Approve tab has per-tool Allow/Ask/Deny with granular exceptions
- CLI handles its own permission model — the extension maps to CLI's `config.permission`
- No YOLO mode toggle
- No task budget limits (requests or cost)
- No command timeout settings
- No keyboard shortcut for toggling auto-approve

## Remaining Work

- **Evaluate YOLO mode**: Determine if a "fully autonomous" toggle makes sense with the CLI backend's permission model. The CLI may need a "approve all" config mode
- **Task budget limits**: If the CLI supports or can support max requests/cost per session, add UI controls. Otherwise, document this as a CLI-side feature request
- **Command timeout**: Determine if the CLI's bash tool has configurable timeout and expose it in settings
- **Auto-approve keybinding**: Add a VS Code command + keybinding to toggle between permissive and restrictive permission profiles
- **YOLO Gatekeeper**: Evaluate whether a secondary AI gatekeeper is feasible with the CLI architecture (would require CLI-side support for dual-model evaluation)

## Notes

The new extension's permission model is fundamentally different from the legacy. The legacy used boolean toggles per action type stored in global state. The new extension uses the CLI's structured permission config with Allow/Ask/Deny per tool and path/command pattern exceptions. Some legacy concepts (YOLO mode, cost limits) may need CLI-side support before the extension can expose them.
