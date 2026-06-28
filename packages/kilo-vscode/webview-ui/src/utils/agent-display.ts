import type { AgentInfo } from "../types/messages"

const descriptions: Record<string, string> = {
  ask: "agent.description.ask",
  code: "agent.description.code",
  debug: "agent.description.debug",
  explore: "agent.description.explore",
  general: "agent.description.general",
  plan: "agent.description.plan",
}

const hidden = new Set(["orchestrator"])

export function isHiddenAgent(name: string): boolean {
  return hidden.has(name)
}

export function agentLabel(agent: AgentInfo): string {
  if (agent.displayName) return agent.displayName
  return agent.name
    .split(/[-_]/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
}

export function agentDescription(agent: AgentInfo, t: (key: string) => string): string | undefined {
  const key = descriptions[agent.name]
  return key ? t(key) : agent.description
}
