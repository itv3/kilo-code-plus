import { expect, test } from "bun:test"
import { DropMaxTokensForGpt5Plugin } from "@/kilocode/plugin/drop-max-tokens"

const pluginInput = {
  client: {} as never,
  project: {} as never,
  directory: "",
  worktree: "",
  experimental_workspace: { register() {} },
  serverUrl: new URL("https://example.com"),
  $: {} as never,
}

function makeHookInput(overrides: { providerID?: string; apiId?: string; npm?: string; reasoning?: boolean }) {
  return {
    sessionID: "s",
    agent: "a",
    provider: {} as never,
    message: {} as never,
    model: {
      providerID: overrides.providerID ?? "litellm",
      api: {
        id: overrides.apiId ?? "gpt-5",
        url: "",
        npm: overrides.npm ?? "@ai-sdk/openai-compatible",
      },
      capabilities: {
        reasoning: overrides.reasoning ?? true,
        temperature: false,
        attachment: true,
        toolcall: true,
        input: { text: true, audio: false, image: false, video: false, pdf: false },
        output: { text: true, audio: false, image: false, video: false, pdf: false },
        interleaved: false,
      },
    } as never,
  }
}

function makeHookOutput() {
  return { temperature: 0, topP: 1, topK: 0, maxOutputTokens: 32_000 as number | undefined, options: {} }
}

test("drops maxOutputTokens for gpt-5 on @ai-sdk/openai-compatible", async () => {
  const hooks = await DropMaxTokensForGpt5Plugin(pluginInput)
  const out = makeHookOutput()
  await hooks["chat.params"]!(makeHookInput({ apiId: "gpt-5" }), out)
  expect(out.maxOutputTokens).toBeUndefined()
})

test("drops maxOutputTokens for gpt-5 variants (codex, pro, mini) on openai-compatible", async () => {
  const hooks = await DropMaxTokensForGpt5Plugin(pluginInput)
  for (const id of ["gpt-5.2-codex", "gpt-5-pro", "gpt-5-mini", "openai/gpt-5-turbo"]) {
    const out = makeHookOutput()
    await hooks["chat.params"]!(makeHookInput({ apiId: id }), out)
    expect(out.maxOutputTokens).toBeUndefined()
  }
})

test("keeps maxOutputTokens for gpt-5 on non-openai-compatible SDKs", async () => {
  const hooks = await DropMaxTokensForGpt5Plugin(pluginInput)
  const out = makeHookOutput()
  await hooks["chat.params"]!(makeHookInput({ apiId: "gpt-5", npm: "@ai-sdk/openai" }), out)
  expect(out.maxOutputTokens).toBe(32_000)
})

test("keeps maxOutputTokens for non-gpt-5 models on openai-compatible", async () => {
  const hooks = await DropMaxTokensForGpt5Plugin(pluginInput)
  const out = makeHookOutput()
  await hooks["chat.params"]!(makeHookInput({ apiId: "gpt-4-turbo" }), out)
  expect(out.maxOutputTokens).toBe(32_000)
})
