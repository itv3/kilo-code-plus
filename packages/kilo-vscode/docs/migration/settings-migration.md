# Settings Migration from Old Extension

**Priority:** P1
**Issue:** [#6089](https://github.com/Kilo-Org/kilocode/issues/6089)

## Remaining Work

- On first activation, detect whether old extension settings exist in `vscode.ExtensionContext.globalState` or `vscode.workspace.getConfiguration('kilo-code')`
- Read relevant settings: API keys, provider configuration, model preferences, auto-approve rules, custom instructions
- Map old settings keys to CLI config equivalents in `opencode.json`
- If CLI config already has settings, show a diff and ask user to confirm before overwriting
- Write approved settings to CLI config via `/global/config` endpoint or directly to `opencode.json`
- Show what was migrated and what was not
- Mark migration as complete in `globalState` so it doesn't run again

## Agent Behaviour Settings to Migrate

The old extension stored many agent behaviour settings in global state. Key mappings:

| Legacy Setting                                                 | CLI Equivalent                                         | Notes                                                                      |
| -------------------------------------------------------------- | ------------------------------------------------------ | -------------------------------------------------------------------------- |
| `customInstructions` (global)                                  | Per-agent `prompt` or rule files in `.kilocode/rules/` | May need a global instructions config key in CLI                           |
| `customModePrompts` (per-mode overrides)                       | Per-agent `prompt`, `temperature`, etc.                | Map each mode slug to CLI agent name                                       |
| `customModes` (custom mode definitions)                        | CLI agent config in `opencode.json`                    | Map `roleDefinition` → `prompt`, `groups` → CLI tool permissions           |
| `alwaysAllowReadOnly`, `alwaysAllowWrite`, etc.                | `config.permission` (Allow/Ask/Deny per tool)          | Different permission model — map boolean toggles to structured permissions |
| `allowedCommands` / `deniedCommands`                           | `config.permission.bash` patterns                      | Map command prefix lists to CLI bash permission patterns                   |
| `modelTemperature`, `modelMaxTokens`, `modelMaxThinkingTokens` | Per-agent `temperature`, `top_p`, `steps`, `variant`   | Map per-profile settings to per-agent config                               |
| `modeApiConfigs` (per-mode model)                              | Per-agent `model`                                      | Map mode slug → agent name → model ID                                      |
| `autoCondenseContext`, `autoCondenseContextPercent`            | `config.compaction.auto`                               | Threshold config may not have CLI equivalent                               |
| `experiments`                                                  | `config.experimental.*`                                | Map relevant experiment flags to CLI experimental settings                 |
| `language`                                                     | Handled by Language tab                                | Already implemented                                                        |
| `browserToolEnabled`, `browserViewportSize`                    | VS Code extension settings                             | Browser settings stay extension-side                                       |

See [Agent Behaviour Feature Parity](../agent-behaviour/) docs for details on each feature area.
