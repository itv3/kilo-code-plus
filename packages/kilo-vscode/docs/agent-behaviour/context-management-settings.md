# Context Management Settings

**Priority:** P2

The legacy extension had granular control over what context was included in agent conversations. The new extension has basic compaction settings but is missing several context management controls.

## Legacy Features

### File Reading Limits

- `maxReadFileLine` — max lines per file read (default: 500)
- `maxImageFileSize` — max image file size included in context
- `maxTotalImageSize` — max total image size across all images
- `maxConcurrentFileReads` — concurrent file read limit
- `allowVeryLargeReads` — toggle to allow reading very large files

### Workspace Context

- `maxOpenTabsContext` — max open editor tabs included in context (default: 20)
- `maxWorkspaceFiles` — max workspace files listed in environment details (default: 200)
- `maxGitStatusFiles` — max git status entries in environment details (default: 0)

### Diagnostic Context

- `includeDiagnosticMessages` — include VS Code diagnostic messages in tool outputs (default: true)
- `maxDiagnosticMessages` — max diagnostic messages to include (default: 50)

### Environment Details

- `includeCurrentTime` — include timestamp in environment details (default: true)
- `includeCurrentCost` — include running cost in environment details (default: true)

### Auto-Condense

- `autoCondenseContext` — automatically condense conversation at context limit (default: true)
- `autoCondenseContextPercent` — threshold percentage for triggering condensation (default: 100)
- `customCondensingPrompt` — custom prompt template for condensation
- `condensingApiConfigId` — separate model for condensation (cheaper/faster model)

## Current State in New Extension

- Context tab has:
  - Auto compaction toggle (`config.compaction.auto`)
  - Prune old tool outputs toggle (`config.compaction.prune`)
  - Watcher ignore patterns
- No file reading limit controls
- No workspace context limits
- No diagnostic context settings
- No condense threshold or custom prompt

## Remaining Work

- **Audit CLI context handling**: Determine which context limits the CLI manages internally vs. what's configurable:
  - Does the CLI have file read size limits?
  - Does the CLI include editor tabs or workspace file lists in context?
  - Does the CLI have diagnostic integration?
- **Compaction settings**: If the CLI supports compaction threshold configuration (e.g., trigger at 80% of context window), expose it in the Context tab
- **Watcher/ignore patterns**: Already implemented — verify coverage matches legacy's include/exclude patterns
- **Diagnostic integration**: If the CLI's LSP integration provides diagnostics in tool outputs, add a toggle to control this

## Notes

The CLI handles context differently from the legacy extension. The legacy extension built context from VS Code APIs (open tabs, diagnostics, workspace files). The CLI may not have access to these VS Code-specific context sources. Some settings may not apply in the CLI architecture, while others may need CLI-side implementation.
