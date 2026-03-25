# MCP Server Creation & Editing

**Priority:** P2

The MCP Servers sub-tab in Agent Behaviour settings can only view server names/commands and remove servers. There is no UI to add or edit MCP server configurations.

## Legacy Features

### MCP Configuration UI

- Add new MCP servers with command, args, and environment variables
- Edit existing server configurations
- Per-server tool allowlisting (select which tools to auto-approve)
- Connection status display per server (connected/disconnected/error)
- Enable/disable individual servers without removing them

### MCP Hub

- Browse and install MCP servers from a registry
- One-click install with configuration

## Current State in New Extension

- MCP Servers sub-tab shows server name and command/URL
- Remove button per server
- No add/create UI in settings
- MCP servers can be installed from the Marketplace (separate feature)
- No connection status display
- No per-server tool allowlisting
- The CLI owns MCP lifecycle — configuration is in `opencode.json`

## Remaining Work

- **Add MCP Server dialog**: Form to define a new MCP server with:
  - Server name
  - Transport type (stdio command vs. SSE URL)
  - Command and arguments (for stdio)
  - URL (for SSE/streamable HTTP)
  - Environment variables
- **Edit MCP Server**: Allow modifying existing server configurations
- **Connection status**: Display per-server connection state (connected/disconnected/error) — requires CLI to expose this via SSE or API
- **Enable/disable toggle**: Per-server toggle without removing the configuration
- **Per-server tool allowlisting**: If the CLI supports per-server tool allow/deny, expose it in the server detail view
- **Write to CLI config**: Use the config update endpoint to persist MCP server changes

## Notes

The Marketplace (marketplace.md) covers browsing and installing MCP servers from a registry. This document covers manual configuration — adding custom/private MCP servers and editing their settings. The `packages/app/` reference implementation may have MCP server management UI that can be ported.
