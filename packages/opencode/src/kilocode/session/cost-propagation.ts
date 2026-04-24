// kilocode_change - new file
import { Effect } from "effect"
import { Session } from "@/session"
import { MessageV2 } from "@/session/message-v2"
import { SessionID, MessageID } from "@/session/schema"

export namespace KiloCostPropagation {
  /**
   * Total assistant-message cost in a session. Because each subagent propagates
   * its own total into the parent assistant message when it finishes, this sum
   * already reflects descendant sessions recursively — no tree walk needed.
   */
  export const childCost = Effect.fn("KiloCostPropagation.childCost")(function* (
    sessions: Session.Interface,
    id: SessionID,
  ) {
    const msgs = yield* sessions.messages({ sessionID: id })
    return msgs.reduce((sum, m) => sum + (m.info.role === "assistant" ? m.info.cost : 0), 0)
  })

  /**
   * Add `amount` to the given parent assistant message's cost. No-op when
   * `amount` is non-positive or the target is not an assistant message.
   *
   * Caller must guarantee serial access to the parent message — concurrent
   * calls against the same `(sid, mid)` are not atomic (read-modify-write).
   */
  export const propagate = Effect.fn("KiloCostPropagation.propagate")(function* (
    sessions: Session.Interface,
    sid: SessionID,
    mid: MessageID,
    amount: number,
  ) {
    if (!(amount > 0)) return
    const parent = yield* Effect.sync(() => MessageV2.get({ sessionID: sid, messageID: mid }))
    if (parent.info.role !== "assistant") return
    parent.info.cost += amount
    yield* sessions.updateMessage(parent.info)
  })
}
