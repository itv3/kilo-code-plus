import { describe, expect, it } from "bun:test"
import { agentDescription, agentLabel, isHiddenAgent } from "../../webview-ui/src/utils/agent-display"
import type { AgentInfo } from "../../webview-ui/src/types/messages"

function agent(input: Partial<AgentInfo>): AgentInfo {
  return {
    name: "code",
    mode: "primary",
    ...input,
  }
}

describe("agent display", () => {
  it("uses localized descriptions for native agents", () => {
    const item = agent({ name: "code", description: "The default agent." })
    expect(agentDescription(item, (key) => `zh:${key}`)).toBe("zh:agent.description.code")
  })

  it("keeps custom agent descriptions", () => {
    const item = agent({ name: "reviewer", description: "Reviews code" })
    expect(agentDescription(item, (key) => key)).toBe("Reviews code")
  })

  it("uses displayName before formatting a slug", () => {
    expect(agentLabel(agent({ name: "custom-agent", displayName: "Custom Agent" }))).toBe("Custom Agent")
  })

  it("formats slugs when displayName is missing", () => {
    expect(agentLabel(agent({ name: "custom-agent" }))).toBe("Custom Agent")
  })

  it("marks orchestrator as hidden from visible lists", () => {
    expect(isHiddenAgent("orchestrator")).toBe(true)
    expect(isHiddenAgent("code")).toBe(false)
  })
})
