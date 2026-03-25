# Terminal Tab Settings

**Priority:** P2

The Terminal tab in Settings shows "Not implemented". The legacy extension had extensive terminal behaviour settings.

## Legacy Features

### Output Limits

- `terminalOutputLineLimit` — max terminal output lines (default: 500)
- `terminalOutputCharacterLimit` — max terminal output characters (default: 50,000)

### Shell Integration

- `terminalShellIntegrationTimeout` — timeout for shell integration detection (default: 30000ms)
- `terminalShellIntegrationDisabled` — disable shell integration entirely

### Command Execution

- `terminalCommandDelay` — delay before command execution
- `commandExecutionTimeout` — global command timeout (0-600 seconds)
- `commandTimeoutAllowlist` — commands exempt from timeout

### Shell-Specific Workarounds

- `terminalPowershellCounter` — PowerShell counter workaround
- `terminalZshClearEolMark` — Zsh EOL mark clearing
- `terminalZshOhMy` — Oh My Zsh compatibility mode
- `terminalZshP10k` — Powerlevel10k compatibility mode
- `terminalZdotdir` — ZDOTDIR handling

### Output Processing

- `terminalCompressProgressBar` — compress progress bar output to save context tokens

## Current State in New Extension

- Terminal tab shows "Not implemented" placeholder
- The CLI's `bash` tool handles command execution internally
- The CLI may have its own terminal output processing

## Remaining Work

- **Audit CLI bash tool configuration**: Determine what terminal-related settings the CLI exposes:
  - Does the CLI have configurable output limits for the bash tool?
  - Does the CLI have command timeout support?
  - Does the CLI handle shell-specific workarounds?
- **Implement Terminal tab**: Based on CLI capabilities, add settings for:
  - Command timeout (if CLI supports it)
  - Output truncation limits (if configurable)
  - Any CLI-exposed bash tool options
- **VS Code terminal integration**: If the extension uses VS Code terminals (e.g., Agent Manager sessions), add settings for terminal behaviour in that context

## Notes

Many legacy terminal settings were workarounds for VS Code's shell integration quirks (Zsh, PowerShell). The CLI executes commands differently (not through VS Code terminals), so these shell-specific workarounds may not apply. Focus on settings that affect the CLI's bash tool behaviour rather than porting legacy workarounds.
