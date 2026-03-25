# Formatter & LSP Detailed Configuration

**Priority:** P3

The extension has simple on/off toggles for the CLI's formatter and LSP features, but the CLI supports detailed per-language and per-server configuration that isn't exposed.

## CLI Capabilities

### Formatter

The CLI's `config.formatter` supports per-language configuration:

- `command` — formatter command to run
- `extensions` — file extensions this formatter handles
- `environment` — environment variables for the formatter process
- `disabled` — disable specific formatters

### LSP

The CLI's `config.lsp` supports per-server configuration:

- `command` — LSP server command
- `extensions` — file extensions this server handles
- `disabled` — disable specific servers
- `env` — environment variables
- `initialization` — initialization options for the server

## Current State in New Extension

- Experimental tab has a simple toggle for formatter (on/off)
- Experimental tab has a simple toggle for LSP (on/off)
- No per-language or per-server configuration

## Remaining Work

- **Formatter configuration UI**: If users need to customize formatters beyond on/off, add a section to configure per-language formatter commands and extensions
- **LSP configuration UI**: If users need to add custom LSP servers or override defaults, add a section to configure per-server settings
- **Evaluate necessity**: These may be power-user features that most users won't need — the CLI's defaults may be sufficient. Determine if there are user requests or use cases that require this level of configuration

## Notes

This is lower priority because the CLI auto-detects formatters and LSP servers for common languages. Manual configuration is mainly needed for custom toolchains or non-standard setups. The `opencode.json` file can be edited directly for these cases.
