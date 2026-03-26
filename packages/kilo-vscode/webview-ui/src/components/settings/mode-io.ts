import type { AgentConfig, PermissionConfig } from "../../types/messages"

/** Maximum import file size in bytes (1 MB). */
export const MAX_IMPORT_SIZE = 1_048_576

const NAME_RE = /^[a-z][a-z0-9-]*$/
const MODES = ["subagent", "primary", "all"] as const
const LEVELS = new Set(["allow", "ask", "deny"])

export type ImportError = "invalidJson" | "invalidName" | "nameTaken" | "tooLarge"

export type ImportResult = { ok: true; name: string; config: AgentConfig } | { ok: false; error: ImportError }

/** Validate and extract a permission map, dropping unknown entries. */
function parsePermission(raw: unknown): PermissionConfig | undefined {
  if (typeof raw !== "object" || raw === null || Array.isArray(raw)) return undefined
  const out: PermissionConfig = {}
  let count = 0
  for (const [key, val] of Object.entries(raw)) {
    if (typeof val === "string" && LEVELS.has(val)) {
      out[key] = val as PermissionConfig[string]
      count++
    }
  }
  return count > 0 ? out : undefined
}

/**
 * Parse a raw JSON string into a validated agent name + config.
 * Returns an error tag (matching the i18n key suffix) on failure.
 */
export function parseImport(json: string, taken: string[]): ImportResult {
  let data: Record<string, unknown>
  try {
    data = JSON.parse(json)
  } catch {
    return { ok: false, error: "invalidJson" }
  }

  const name = typeof data.name === "string" ? data.name.trim() : ""
  if (!name || !NAME_RE.test(name)) {
    return { ok: false, error: "invalidName" }
  }
  if (taken.includes(name)) {
    return { ok: false, error: "nameTaken" }
  }

  const partial: Partial<AgentConfig> = {}
  if (typeof data.description === "string") partial.description = data.description
  if (typeof data.prompt === "string") partial.prompt = data.prompt
  if (typeof data.model === "string") partial.model = data.model
  if (typeof data.mode === "string" && (MODES as readonly string[]).includes(data.mode))
    partial.mode = data.mode as AgentConfig["mode"]
  if (typeof data.temperature === "number") partial.temperature = data.temperature
  if (typeof data.top_p === "number") partial.top_p = data.top_p
  if (typeof data.steps === "number") partial.steps = data.steps
  const perms = parsePermission(data.permission)
  if (perms) partial.permission = perms

  return {
    ok: true,
    name,
    config: { ...partial, mode: partial.mode ?? "primary" },
  }
}

/**
 * Build the JSON-serialisable export payload for a mode.
 */
export function buildExport(name: string, cfg: AgentConfig): Record<string, unknown> {
  return { name, ...cfg }
}
