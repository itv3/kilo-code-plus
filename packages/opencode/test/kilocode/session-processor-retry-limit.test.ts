import { afterEach, describe, expect, mock, spyOn, test } from "bun:test"

mock.module("@/kilo-sessions/remote-sender", () => ({
  RemoteSender: {
    create() {
      return {
        queue() {},
        flush: async () => undefined,
      }
    },
  },
}))

import { APICallError } from "ai"
import { Bus } from "../../src/bus"
import { Flag } from "../../src/flag/flag"
import { Identifier } from "../../src/id/id"
import { Instance } from "../../src/project/instance"
import type { Provider } from "../../src/provider/provider"
import { LLM } from "../../src/session/llm"
import type { LLM as LLMType } from "../../src/session/llm"
import { MessageV2 } from "../../src/session/message-v2"
import { SessionRetry } from "../../src/session/retry"
import { SessionStatus } from "../../src/session/status"
import { Log } from "../../src/util/log"
import { tmpdir } from "../fixture/fixture"

Log.init({ print: false })

function createModel(): Provider.Model {
  return {
    id: "gpt-4",
    providerID: "openai",
    name: "GPT-4",
    limit: {
      context: 128000,
      input: 0,
      output: 4096,
    },
    cost: { input: 0, output: 0, cache: { read: 0, write: 0 } },
    capabilities: {
      toolcall: true,
      attachment: false,
      reasoning: false,
      temperature: true,
      input: { text: true, image: false, audio: false, video: false },
      output: { text: true, image: false, audio: false, video: false },
    },
    api: { id: "openai", url: "https://api.openai.com/v1", npm: "@ai-sdk/openai" },
    options: {},
    headers: {},
  } as Provider.Model
}

function retryable429() {
  return new APICallError({
    message: "429 status code (no body)",
    url: "https://api.openai.com/v1/chat/completions",
    requestBodyValues: {},
    statusCode: 429,
    responseHeaders: { "content-type": "application/json" },
    isRetryable: true,
  })
}

function sentinel() {
  return new Error("unexpected extra llm call")
}

async function seed(model: Provider.Model) {
  const { Session } = await import("../../src/session")
  const session = await Session.create({})
  const user = (await Session.updateMessage({
    id: Identifier.ascending("message"),
    role: "user",
    sessionID: session.id,
    time: { created: Date.now() },
    agent: "code",
    model: { providerID: model.providerID, modelID: model.id },
    tools: {},
  })) as MessageV2.User
  const assistant = (await Session.updateMessage({
    id: Identifier.ascending("message"),
    parentID: user.id,
    role: "assistant",
    mode: "code",
    agent: "code",
    path: {
      cwd: Instance.directory,
      root: Instance.worktree,
    },
    cost: 0,
    tokens: {
      input: 0,
      output: 0,
      reasoning: 0,
      cache: { read: 0, write: 0 },
    },
    modelID: model.id,
    providerID: model.providerID,
    time: { created: Date.now() },
    sessionID: session.id,
  })) as MessageV2.Assistant
  return { assistant, session, user }
}

function input(model: Provider.Model, sessionID: string, user: MessageV2.User): LLMType.StreamInput {
  return {
    user,
    sessionID,
    model,
    agent: { name: "code", mode: "primary", permission: [], options: {} } as any,
    system: [],
    abort: AbortSignal.any([]),
    messages: [],
    tools: {},
  }
}

afterEach(() => {
  delete process.env.KILO_SESSION_RETRY_LIMIT
})

describe("session processor retry limit", () => {
  test("stops after two retries with the normalized retryable error", async () => {
    await using tmp = await tmpdir({ git: true })
    process.env.KILO_SESSION_RETRY_LIMIT = "2"

    await Instance.provide({
      directory: tmp.path,
      fn: async () => {
        const { Session } = await import("../../src/session")
        const { SessionProcessor } = await import("../../src/session/processor")
        const model = createModel()
        const seeded = await seed(model)
        const retry: number[] = []
        const errors: Array<MessageV2.Assistant["error"]> = []
        const unsubStatus = Bus.subscribe(SessionStatus.Event.Status, (event) => {
          if (event.properties.sessionID !== seeded.session.id) return
          if (event.properties.status.type !== "retry") return
          retry.push(event.properties.status.attempt)
        })
        const unsubError = Bus.subscribe(Session.Event.Error, (event) => {
          if (event.properties.sessionID !== seeded.session.id) return
          errors.push(event.properties.error)
        })
        const llm = spyOn(LLM, "stream")
          .mockRejectedValueOnce(retryable429())
          .mockRejectedValueOnce(retryable429())
          .mockRejectedValueOnce(retryable429())
          .mockRejectedValue(sentinel())
        const sleep = spyOn(SessionRetry, "sleep").mockResolvedValue(undefined)
        const processor = SessionProcessor.create({
          assistantMessage: seeded.assistant,
          sessionID: seeded.session.id,
          model,
          abort: AbortSignal.any([]),
        })

        try {
          const result = await processor.process(input(model, seeded.session.id, seeded.user))
          const expected = MessageV2.fromError(retryable429(), { providerID: "openai" })

          expect(result).toBe("stop")
          expect(llm).toHaveBeenCalledTimes(3)
          expect(sleep).toHaveBeenCalledTimes(2)
          expect(retry).toStrictEqual([1, 2])
          expect(processor.message.error).toStrictEqual(expected)
          expect(errors).toStrictEqual([expected])
        } finally {
          unsubStatus()
          unsubError()
          llm.mockRestore()
          sleep.mockRestore()
        }
      },
    })
  })

  test("only positive integers enable the limit", () => {
    delete process.env.KILO_SESSION_RETRY_LIMIT
    expect(Flag.KILO_SESSION_RETRY_LIMIT).toBeUndefined()

    process.env.KILO_SESSION_RETRY_LIMIT = "0"
    expect(Flag.KILO_SESSION_RETRY_LIMIT).toBeUndefined()

    process.env.KILO_SESSION_RETRY_LIMIT = "-1"
    expect(Flag.KILO_SESSION_RETRY_LIMIT).toBeUndefined()

    process.env.KILO_SESSION_RETRY_LIMIT = "abc"
    expect(Flag.KILO_SESSION_RETRY_LIMIT).toBeUndefined()

    process.env.KILO_SESSION_RETRY_LIMIT = "2"
    expect(Flag.KILO_SESSION_RETRY_LIMIT).toBe(2)
  })
})
