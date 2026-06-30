import type { Provider, ProviderModel } from "../../types/messages"
import type { CustomProviderPackage } from "../../../../src/shared/provider-model"

export type CustomProviderDefaults = {
  image?: boolean
  reasoning?: boolean
  contextLimit?: number
  outputLimit?: number
  inputCost?: number
  outputCost?: number
  cacheReadCost?: number
  cacheWriteCost?: number
}

const FALLBACKS: Record<string, string> = {
  "claude-opus-4-8": "claude-opus-4-7",
}

const SUFFIXES = ["-thinking", "-reasoning"]

export function catalogProvider(npm: CustomProviderPackage) {
  if (npm === "@ai-sdk/anthropic") return "anthropic"
  if (npm === "@ai-sdk/google") return "google"
  return "openai"
}

export function catalogDefaults(model: ProviderModel | undefined): CustomProviderDefaults {
  if (!model) return {}
  return {
    image: model.capabilities?.input?.image,
    reasoning: model.capabilities?.reasoning,
    contextLimit: model.limit?.context,
    outputLimit: model.limit?.output,
    inputCost: model.cost?.input,
    outputCost: model.cost?.output,
    cacheReadCost: model.cost?.cache?.read,
    cacheWriteCost: model.cost?.cache?.write,
  }
}

function add(list: string[], key: string | undefined) {
  if (!key || list.includes(key)) return
  list.push(key)
}

function bare(id: string) {
  return id.split("/").at(-1) ?? id
}

export function defaultKeys(id: string) {
  const list: string[] = []
  const base = [id, bare(id)]
  for (const key of base) {
    add(list, key)
    for (const suffix of SUFFIXES) {
      if (key.endsWith(suffix)) add(list, key.slice(0, -suffix.length))
    }
  }
  for (const key of [...list]) add(list, FALLBACKS[key])
  return list
}

export function defaultsForModel(providers: Record<string, Provider>, npm: CustomProviderPackage, id: string) {
  const models = providers[catalogProvider(npm)]?.models ?? {}
  for (const key of defaultKeys(id)) {
    const model = models[key]
    if (model) return catalogDefaults(model)
  }
  return {}
}
