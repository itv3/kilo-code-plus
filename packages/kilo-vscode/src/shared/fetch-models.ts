/**
 * 从提供商原生模型发现接口读取可用模型。
 * 运行在扩展宿主中，不依赖 CLI backend。
 */

export type FetchModelsProtocol = "openai" | "anthropic" | "gemini"

type Options = {
  baseURL: string
  apiKey?: string
  headers?: Record<string, string>
  protocol: FetchModelsProtocol
}

type ModelEntry = {
  id: string
  name: string
  contextLimit?: number
  outputLimit?: number
  inputCost?: number
  outputCost?: number
  cacheReadCost?: number
  cacheWriteCost?: number
}

export class FetchModelsError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message)
    this.name = "FetchModelsError"
  }

  get auth() {
    return this.status === 401 || this.status === 403
  }
}

function limit(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) && value > 0 ? value : undefined
}

function num(value: unknown) {
  const n = typeof value === "string" && value.trim() ? Number(value) : value
  return typeof n === "number" && Number.isFinite(n) && n >= 0 ? n : undefined
}

function perMillion(value: unknown) {
  const n = num(value)
  return n === undefined ? undefined : n * 1_000_000
}

function trim(value: unknown) {
  return typeof value === "string" ? value.trim() : ""
}

function record(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? (value as Record<string, unknown>) : {}
}

function cost(item: Record<string, unknown>): Partial<ModelEntry> {
  const cfg = record(item.cost)
  const pricing = record(item.pricing)
  const input =
    num(cfg.input) ?? perMillion(pricing.prompt) ?? perMillion(item.input_cost_per_token) ?? perMillion(item.prompt_cost_per_token)
  const output =
    num(cfg.output) ??
    perMillion(pricing.completion) ??
    perMillion(item.output_cost_per_token) ??
    perMillion(item.completion_cost_per_token)
  const cacheRead =
    num(cfg.cache_read) ??
    perMillion(pricing.input_cache_read) ??
    perMillion(item.cache_read_cost_per_token) ??
    perMillion(item.input_cache_read_cost_per_token)
  const cacheWrite =
    num(cfg.cache_write) ??
    perMillion(pricing.input_cache_write) ??
    perMillion(item.cache_write_cost_per_token) ??
    perMillion(item.input_cache_write_cost_per_token)
  return {
    ...(input !== undefined ? { inputCost: input } : {}),
    ...(output !== undefined ? { outputCost: output } : {}),
    ...(cacheRead !== undefined ? { cacheReadCost: cacheRead } : {}),
    ...(cacheWrite !== undefined ? { cacheWriteCost: cacheWrite } : {}),
  }
}

async function request(url: string, headers: Record<string, string>) {
  const response = await fetch(url, {
    method: "GET",
    headers,
    signal: AbortSignal.timeout(15_000),
  })

  if (!response.ok) {
    const text = await response.text().catch(() => "")
    throw new FetchModelsError(`HTTP ${response.status}: ${text.slice(0, 200)}`, response.status)
  }

  return response.json()
}

function sort(models: ModelEntry[]) {
  return models.sort((a, b) => a.id.localeCompare(b.id))
}

function unique(models: ModelEntry[]) {
  const seen = new Set<string>()
  const result: ModelEntry[] = []
  for (const model of models) {
    if (!model.id || seen.has(model.id)) continue
    seen.add(model.id)
    result.push(model)
  }
  return sort(result)
}

async function fetchOpenAIModels(opts: Options): Promise<ModelEntry[]> {
  const url = opts.baseURL.replace(/\/+$/, "") + "/models"
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...opts.headers,
  }
  if (opts.apiKey) {
    headers["Authorization"] = `Bearer ${opts.apiKey}`
  }

  const body = (await request(url, headers)) as { data?: Array<Record<string, unknown>> }
  const items = body?.data
  if (!Array.isArray(items)) return []

  return unique(
    items.map((item) => {
      const id = trim(item.id)
      const name = trim(item.name) || id
      return {
        id,
        name,
        contextLimit: limit(item.context_length),
        outputLimit: limit(item.max_output_tokens),
        ...cost(item),
      }
    }),
  )
}

async function fetchAnthropicModels(opts: Options): Promise<ModelEntry[]> {
  const url = opts.baseURL.replace(/\/+$/, "") + "/models"
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    "anthropic-version": "2023-06-01",
    ...opts.headers,
  }
  if (opts.apiKey) {
    headers["x-api-key"] = opts.apiKey
  }

  const body = (await request(url, headers)) as { data?: Array<Record<string, unknown>> }
  const items = body?.data
  if (!Array.isArray(items)) return []

  return unique(
    items.map((item) => {
      const id = trim(item.id)
      const name = trim(item.display_name) || trim(item.name) || id
      return {
        id,
        name,
        contextLimit: limit(item.context_window),
        outputLimit: limit(item.max_output_tokens),
        ...cost(item),
      }
    }),
  )
}

async function fetchGeminiModels(opts: Options): Promise<ModelEntry[]> {
  const url = opts.baseURL.replace(/\/+$/, "") + "/models"
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...opts.headers,
  }
  if (opts.apiKey) {
    headers["x-goog-api-key"] = opts.apiKey
  }

  const body = (await request(url, headers)) as { models?: Array<Record<string, unknown>> }
  const items = body?.models
  if (!Array.isArray(items)) return []

  return unique(
    items.map((item) => {
      const raw = trim(item.name)
      const id = raw.replace(/^models\//, "")
      const name = trim(item.displayName) || id
      return {
        id,
        name,
        contextLimit: limit(item.inputTokenLimit),
        outputLimit: limit(item.outputTokenLimit),
        ...cost(item),
      }
    }),
  )
}

export async function fetchModels(opts: Options): Promise<ModelEntry[]> {
  if (opts.protocol === "anthropic") return fetchAnthropicModels(opts)
  if (opts.protocol === "gemini") return fetchGeminiModels(opts)
  return fetchOpenAIModels(opts)
}
