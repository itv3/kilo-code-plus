import type { AgentInfo } from "../types/messages"
import { HIDDEN_AGENT_NAMES } from "../../../src/shared/agents"

const descriptions: Record<string, string> = {
  ask: "agent.description.ask",
  code: "agent.description.code",
  debug: "agent.description.debug",
  explore: "agent.description.explore",
  general: "agent.description.general",
  plan: "agent.description.plan",
}

export function isHiddenAgent(agent: Pick<AgentInfo, "name" | "native"> | string): boolean {
  const name = typeof agent === "string" ? agent : agent.name
  const native = typeof agent === "string" ? true : agent.native !== false
  return native && HIDDEN_AGENT_NAMES.has(name)
}

export function agentLabel(agent: AgentInfo): string {
  if (agent.displayName) return agent.displayName
  return agent.name
    .split(/[-_]/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
}

export function agentDescription(agent: AgentInfo, t: (key: string) => string): string | undefined {
  const key = agent.native === true ? descriptions[agent.name] : undefined
  return key ? t(key) : agent.description
}
