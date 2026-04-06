import type { QuestionOption } from "../../types/messages"

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
