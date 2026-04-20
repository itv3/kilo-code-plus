import type { QuestionOption } from "../../types/messages"

/**
 * Translate a backend-provided i18n key, falling back to the canonical label when no key is
 * set or the key is missing from the dictionary. Our `language.t` returns the key string back
 * when a translation isn't found, so we detect that and substitute the fallback.
 */
export function tr(translate: (key: string) => string, key: string | undefined, fallback: string): string {
  if (!key) return fallback
  const result = translate(key)
  if (result === key) return fallback
  return result
}

export type PickOutcome = { kind: "submit" } | { kind: "advance" } | { kind: "stay" }

/**
 * Decide what should happen after a user picks an option in the question dock.
 *
 * - Multi-select prompts: the pick only toggles local state; no tab change, no submit.
 * - Single-question single-select, option pick: submit immediately (matches the TUI).
 * - Multi-question single-select, option pick: advance to the next tab.
 * - Custom-input path for a single-select is handled separately in handleCustomSubmit.
 */
export function pickOutcome(input: { single: boolean; multi: boolean; custom: boolean }): PickOutcome {
  if (input.multi) return { kind: "stay" }
  if (input.single && !input.custom) return { kind: "submit" }
  return { kind: "advance" }
}

export function toggleAnswer(existing: string[], answer: string): string[] {
  const next = [...existing]
  const index = next.indexOf(answer)
  if (index === -1) next.push(answer)
  if (index !== -1) next.splice(index, 1)
  return next
}

export function resolveQuestionMode(options: QuestionOption[], answer: string): string | undefined {
  return options.find((item) => item.label === answer)?.mode
}

export function resolveSelectedQuestionMode(
  questions: Array<{ options?: QuestionOption[] }>,
  answers: string[][],
  kinds: Record<string, "option" | "custom">[] = [],
): string | undefined {
  let mode: string | undefined

  for (const [i, list] of answers.entries()) {
    const options = questions[i]?.options ?? []
    for (const answer of list) {
      if (kinds[i]?.[answer] === "custom") continue
      const next = resolveQuestionMode(options, answer)
      if (next) mode = next
    }
  }

  return mode
}

export function resolveOptimisticQuestionAgent(base: string | undefined, current: string, mode: string | undefined) {
  if (!mode) {
    return {
      base: undefined,
      agent: base,
    }
  }

  if (base === undefined && current === mode) {
    return {
      base: undefined,
      agent: undefined,
    }
  }

  return {
    base: base ?? current,
    agent: mode,
  }
}
