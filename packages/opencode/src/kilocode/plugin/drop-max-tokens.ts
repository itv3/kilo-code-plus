import type { Hooks, PluginInput } from "@kilocode/plugin"

// Drop the max output tokens cap for gpt-5 models routed through
// @ai-sdk/openai-compatible. The SDK always emits `max_tokens`, but OpenAI's
// gpt-5 family (and compatible proxies like LiteLLM) rejects that field and
// requires `max_completion_tokens` instead. The openai-compatible SDK has no
// way to rename the field, so we clear the cap and let the upstream default
// output budget apply.
export async function DropMaxTokensForGpt5Plugin(_input: PluginInput): Promise<Hooks> {
  return {
    "chat.params": async (input, output) => {
      if (input.model.api.npm !== "@ai-sdk/openai-compatible") return
      if (!input.model.api.id.toLowerCase().includes("gpt-5")) return
      output.maxOutputTokens = undefined
    },
  }
}
