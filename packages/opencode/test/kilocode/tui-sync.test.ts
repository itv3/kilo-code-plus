import { describe, expect, test } from "bun:test"
import { KiloSessionTuiSync } from "../../src/kilocode/session/tui-sync"

type Message = {
  role: string
  model?: { providerID: string; modelID: string; variant?: string }
  parts?: readonly { type: string }[]
}

function syncVariant(input: { current: string | undefined; message: Message }) {
  if (!KiloSessionTuiSync.model(input.message)) return input.current
  return input.message.model?.variant ?? "default"
}

describe("KiloSessionTuiSync.model", () => {
  test("syncs normal user messages", () => {
    expect(KiloSessionTuiSync.model({ role: "user", parts: [{ type: "text" }] })).toBe(true)
  })

  test("skips compaction marker user messages", () => {
    expect(KiloSessionTuiSync.model({ role: "user", parts: [{ type: "compaction" }] })).toBe(false)
  })

  test("skips messages checked with stored parts", () => {
    const msg = { role: "user" }
    const parts = [{ type: "compaction" }]

    expect(KiloSessionTuiSync.model({ role: msg.role, parts })).toBe(false)
  })

  test("skips non-user messages", () => {
    expect(KiloSessionTuiSync.model({ role: "assistant", parts: [{ type: "text" }] })).toBe(false)
  })

  test("preserves thinking level after /compact", () => {
    const msg = {
      role: "user",
      model: { providerID: "anthropic", modelID: "claude-sonnet-4-5" },
      parts: [{ type: "compaction" }],
    }

    expect(syncVariant({ current: "high", message: msg })).toBe("high")
  })

  test("still updates thinking level from normal user messages", () => {
    const msg = {
      role: "user",
      model: { providerID: "anthropic", modelID: "claude-sonnet-4-5", variant: "max" },
      parts: [{ type: "text" }],
    }

    expect(syncVariant({ current: "high", message: msg })).toBe("max")
  })
})
