import { describe, expect, it } from "bun:test"
import { defaultKeys, defaultsForModel } from "../../webview-ui/src/components/settings/CustomProviderDefaults"
import {
  customProviderCatalog,
  customProviderProtocol,
  normalizeCustomProviderBaseURL,
} from "../../src/shared/provider-model"
import type { Provider } from "../../webview-ui/src/types/messages"

function providers(): Record<string, Provider> {
  return {
    anthropic: {
      id: "anthropic",
      name: "Anthropic",
      models: {
        "claude-opus-4-6": {
          id: "claude-opus-4-6",
          name: "Claude Opus 4.6",
          capabilities: {
            reasoning: true,
            input: { text: true, image: true, audio: false, video: false, pdf: false },
          },
          limit: { context: 200000, output: 32000 },
          cost: { input: 15, output: 75, cache: { read: 1.5, write: 18.75 } },
        },
        "claude-opus-4-7": {
          id: "claude-opus-4-7",
          name: "Claude Opus 4.7",
          capabilities: {
            reasoning: true,
            input: { text: true, image: true, audio: false, video: false, pdf: false },
          },
          limit: { context: 200000, output: 64000 },
          cost: { input: 20, output: 100, cache: { read: 2, write: 25 } },
        },
      },
    },
  }
}

describe("custom provider default matching", () => {
  it("keeps package protocol, catalog, and base URL metadata in one mapping", () => {
    expect(customProviderProtocol("@ai-sdk/openai-compatible")).toBe("openai")
    expect(customProviderCatalog("@ai-sdk/openai-compatible")).toBe("openai")
    expect(customProviderProtocol("@ai-sdk/anthropic")).toBe("anthropic")
    expect(customProviderCatalog("@ai-sdk/anthropic")).toBe("anthropic")
    expect(normalizeCustomProviderBaseURL("@ai-sdk/anthropic", "https://api.anthropic.com")).toBe(
      "https://api.anthropic.com/v1",
    )
    expect(customProviderProtocol("@ai-sdk/google")).toBe("gemini")
    expect(customProviderCatalog("@ai-sdk/google")).toBe("google")
    expect(normalizeCustomProviderBaseURL("@ai-sdk/google", "https://generativelanguage.googleapis.com")).toBe(
      "https://generativelanguage.googleapis.com/v1beta",
    )
  })

  it("matches thinking suffix models against the base catalog model", () => {
    const out = defaultsForModel(providers(), "@ai-sdk/anthropic", "claude-opus-4-6-thinking")

    expect(out.reasoning).toBe(true)
    expect(out.image).toBe(true)
    expect(out.contextLimit).toBe(200000)
    expect(out.outputLimit).toBe(32000)
    expect(out.inputCost).toBe(15)
    expect(out.outputCost).toBe(75)
    expect(out.cacheReadCost).toBe(1.5)
    expect(out.cacheWriteCost).toBe(18.75)
  })

  it("keeps the Opus 4.8 fallback after removing a variant suffix", () => {
    expect(defaultKeys("anthropic/claude-opus-4-8-thinking")).toEqual([
      "anthropic/claude-opus-4-8-thinking",
      "anthropic/claude-opus-4-8",
      "claude-opus-4-8-thinking",
      "claude-opus-4-8",
      "claude-opus-4-7",
    ])
  })
})
