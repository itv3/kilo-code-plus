// kilocode_change - new file
// Command barrel: exports the list of CommandModules with no side effects.
// Keeping this separate from index.ts allows help.ts and generate-cli-docs.ts
// to import commands for introspection without triggering the CLI startup code
// (middleware, telemetry, migrations, etc.) that lives in index.ts.
import { AcpCommand } from "./cmd/acp"
import { McpCommand } from "./cmd/mcp"
import { TuiThreadCommand } from "./cmd/tui/thread"
import { AttachCommand } from "./cmd/tui/attach"
import { RunCommand } from "./cmd/run"
import { GenerateCommand } from "./cmd/generate"
import { DebugCommand } from "./cmd/debug"
import { AuthCommand } from "./cmd/auth"
import { AgentCommand } from "./cmd/agent"
import { UpgradeCommand } from "./cmd/upgrade"
import { UninstallCommand } from "./cmd/uninstall"
import { ServeCommand } from "./cmd/serve"
import { WebCommand } from "./cmd/web"
import { ModelsCommand } from "./cmd/models"
import { StatsCommand } from "./cmd/stats"
import { ExportCommand } from "./cmd/export"
import { ImportCommand } from "./cmd/import"
import { PrCommand } from "./cmd/pr"
import { SessionCommand } from "./cmd/session"
import { RemoteCommand } from "./cmd/remote" // kilocode_change
import { DbCommand } from "./cmd/db"
import { HelpCommand } from "../kilocode/help-command" // kilocode_change

export const commands = [
  AcpCommand,
  McpCommand,
  TuiThreadCommand,
  AttachCommand,
  RunCommand,
  GenerateCommand,
  DebugCommand,
  AuthCommand,
  AgentCommand,
  UpgradeCommand,
  UninstallCommand,
  ServeCommand,
  WebCommand,
  ModelsCommand,
  StatsCommand,
  ExportCommand,
  ImportCommand,
  PrCommand,
  SessionCommand,
  RemoteCommand, // kilocode_change
  DbCommand,
  HelpCommand, // kilocode_change
]
