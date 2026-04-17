import z from "zod"
import { Cause, Effect, Layer, ServiceMap } from "effect" // kilocode_change — Cause.hasInterrupts
import { makeRuntime } from "@/effect/run-service"
import { Bus } from "@/bus"
import { Snapshot } from "@/snapshot"
import { Storage } from "@/storage/storage"
import { Session } from "."
import { MessageV2 } from "./message-v2"
import { SessionID, MessageID } from "./schema"
import { Log } from "@/util/log" // kilocode_change

export namespace SessionSummary {
  const log = Log.create({ service: "session.summary" }) // kilocode_change

  function unquoteGitPath(input: string) {
    if (!input.startsWith('"')) return input
    if (!input.endsWith('"')) return input
    const body = input.slice(1, -1)
    const bytes: number[] = []

    for (let i = 0; i < body.length; i++) {
      const char = body[i]!
      if (char !== "\\") {
        bytes.push(char.charCodeAt(0))
        continue
      }

      const next = body[i + 1]
      if (!next) {
        bytes.push("\\".charCodeAt(0))
        continue
      }

      if (next >= "0" && next <= "7") {
        const chunk = body.slice(i + 1, i + 4)
        const match = chunk.match(/^[0-7]{1,3}/)
        if (!match) {
          bytes.push(next.charCodeAt(0))
          i++
          continue
        }
        bytes.push(parseInt(match[0], 8))
        i += match[0].length
        continue
      }

      const escaped =
        next === "n"
          ? "\n"
          : next === "r"
            ? "\r"
            : next === "t"
              ? "\t"
              : next === "b"
                ? "\b"
                : next === "f"
                  ? "\f"
                  : next === "v"
                    ? "\v"
                    : next === "\\" || next === '"'
                      ? next
                      : undefined

      bytes.push((escaped ?? next).charCodeAt(0))
      i++
    }

    return Buffer.from(bytes).toString()
  }

  export interface Interface {
    readonly summarize: (input: { sessionID: SessionID; messageID: MessageID }) => Effect.Effect<void>
    readonly diff: (input: { sessionID: SessionID; messageID?: MessageID }) => Effect.Effect<Snapshot.FileDiff[]>
    readonly computeDiff: (input: { messages: MessageV2.WithParts[] }) => Effect.Effect<Snapshot.FileDiff[]>
  }

  export class Service extends ServiceMap.Service<Service, Interface>()("@opencode/SessionSummary") {}

  export const layer = Layer.effect(
    Service,
    Effect.gen(function* () {
      const sessions = yield* Session.Service
      const snapshot = yield* Snapshot.Service
      const storage = yield* Storage.Service
      const bus = yield* Bus.Service

      const computeDiff = Effect.fn("SessionSummary.computeDiff")(function* (input: {
        messages: MessageV2.WithParts[]
      }) {
        let from: string | undefined
        let to: string | undefined
        for (const item of input.messages) {
          if (!from) {
            for (const part of item.parts) {
              if (part.type === "step-start" && part.snapshot) {
                from = part.snapshot
                break
              }
            }
          }
          for (const part of item.parts) {
            if (part.type === "step-finish" && part.snapshot) to = part.snapshot
          }
        }
        if (from && to) return yield* snapshot.diffFull(from, to)
        return []
      })

      const summarize = Effect.fn("SessionSummary.summarize")(function* (input: {
        sessionID: SessionID
        messageID: MessageID
      }) {
        const all = yield* sessions.messages({ sessionID: input.sessionID })
        if (!all.length) return

        const diffs = yield* computeDiff({ messages: all })
        yield* sessions.setSummary({
          sessionID: input.sessionID,
          summary: {
            additions: diffs.reduce((sum, x) => sum + x.additions, 0),
            deletions: diffs.reduce((sum, x) => sum + x.deletions, 0),
            files: diffs.length,
          },
        })
        yield* storage.write(["session_diff", input.sessionID], diffs).pipe(Effect.ignore)
        yield* bus.publish(Session.Event.Diff, { sessionID: input.sessionID, diff: diffs })

        const messages = all.filter(
          (m) => m.info.id === input.messageID || (m.info.role === "assistant" && m.info.parentID === input.messageID),
        )
        const target = messages.find((m) => m.info.id === input.messageID)
        if (!target || target.info.role !== "user") return
        const msgDiffs = yield* computeDiff({ messages })
        target.info.summary = { ...target.info.summary, diffs: msgDiffs }
        yield* sessions.updateMessage(target.info)
      })

      const diff = Effect.fn("SessionSummary.diff")(function* (input: { sessionID: SessionID; messageID?: MessageID }) {
        const diffs = yield* storage
          .read<Snapshot.FileDiff[]>(["session_diff", input.sessionID])
          .pipe(Effect.catch(() => Effect.succeed([] as Snapshot.FileDiff[])))
        const next = diffs.map((item) => {
          const file = unquoteGitPath(item.file)

          // kilocode_change start — scrub oversized diffs from stored session_diff
          const oversized = Buffer.byteLength(item.patch) > Snapshot.MAX_DIFF_SIZE
          if (file === item.file && !oversized) return item
          return {
            ...item,
            file,
            patch: oversized ? "" : item.patch,
          }
          // kilocode_change end
        })
        const changed = next.some((item, i) => item.file !== diffs[i]?.file)
        if (changed) yield* storage.write(["session_diff", input.sessionID], next).pipe(Effect.ignore)
        return next
      })

      return Service.of({ summarize, diff, computeDiff })
    }),
  )

  export const defaultLayer = Layer.unwrap(
    Effect.sync(() =>
      layer.pipe(
        Layer.provide(Session.defaultLayer),
        Layer.provide(Snapshot.defaultLayer),
        Layer.provide(Storage.defaultLayer),
        Layer.provide(Bus.layer),
      ),
    ),
  )

  const { runPromise } = makeRuntime(Service, defaultLayer)

  // kilocode_change start — tracked dispatcher + timeout + cancel
  //
  // Before: `summarize` was a naked `void runPromise(...).catch(() => {})`.
  // A huge diff inside the underlying Snapshot call would run synchronously
  // on the TUI worker thread for minutes, starving ESC, heartbeats, and the
  // AI stream. Now:
  //
  //   - Each summary runs under a fresh AbortController tracked in `inflight`.
  //   - A previous in-flight summary for the same sessionID is aborted when
  //     a new one starts, preventing pileup across rapid turns.
  //   - A wall-clock timeout (default 10s) races against the Effect fiber
  //     so we always unwind within a bounded time even on pathological input.
  //   - `cancel(sessionID)` fires `ac.abort()`, which is bridged into
  //     `Effect.interrupt` via a third racer inside `Effect.raceAll`. That is
  //     what makes the ESC path (SessionPrompt.cancel) actually stop the
  //     running fiber rather than just dropping the map entry.
  const SUMMARY_TIMEOUT_MS = 10_000
  const inflight = new Map<SessionID, AbortController>()

  export const summarize = (input: { sessionID: SessionID; messageID: MessageID }) => {
    const prev = inflight.get(input.sessionID)
    if (prev) prev.abort()

    const ac = new AbortController()
    inflight.set(input.sessionID, ac)
    const started = Date.now()

    void runPromise((svc) =>
      // raceAllFirst with three racers: the real work, a wall-clock timeout,
      // and an AbortSignal bridge. Whichever FINISHES FIRST wins (success,
      // failure, or interrupt — `raceAll` would keep waiting on interrupts),
      // and the other two are interrupted by the Effect runtime. The
      // signal-bridge racer is what makes `cancel(sessionID)` actually stop
      // the fiber instead of just removing the map entry.
      Effect.raceAllFirst([
        svc.summarize(input),
        Effect.sleep(`${SUMMARY_TIMEOUT_MS} millis`).pipe(Effect.andThen(Effect.interrupt)),
        Effect.callback<void>((resume) => {
          if (ac.signal.aborted) {
            resume(Effect.interrupt)
            return
          }
          const onAbort = () => resume(Effect.interrupt)
          ac.signal.addEventListener("abort", onAbort, { once: true })
          return Effect.sync(() => ac.signal.removeEventListener("abort", onAbort))
        }),
      ]).pipe(
        Effect.onInterrupt(() =>
          Effect.sync(() => {
            const elapsed = Date.now() - started
            log.warn("summary interrupted", { sessionID: input.sessionID, elapsed })
            Bus.publish(Session.Event.Warning, {
              sessionID: input.sessionID,
              kind: "summary_truncated",
              message: `Session summary interrupted after ${elapsed}ms`,
              details: { elapsed, timeout: SUMMARY_TIMEOUT_MS },
            }).catch(() => {})
          }),
        ),
        Effect.ensuring(
          Effect.sync(() => {
            // Drop the entry only if it is still ours — a newer summarize for
            // the same session may have replaced it already.
            if (inflight.get(input.sessionID) === ac) inflight.delete(input.sessionID)
          }),
        ),
        Effect.catchCause((cause) =>
          Effect.sync(() => {
            // onInterrupt already emitted a warning for the interrupt path.
            // Only emit "summary_failed" for real failures/defects.
            if (Cause.hasInterrupts(cause)) return
            log.warn("summary failed", { sessionID: input.sessionID, cause: String(cause) })
            Bus.publish(Session.Event.Warning, {
              sessionID: input.sessionID,
              kind: "summary_failed",
              message: "Session summary failed",
            }).catch(() => {})
          }),
        ),
      ),
    ).catch(() => {})
  }

  export async function cancel(sessionID: SessionID) {
    const ac = inflight.get(sessionID)
    if (!ac) return
    ac.abort()
    inflight.delete(sessionID)
    log.info("summary cancelled", { sessionID })
  }

  /** Visible for testing — not a stable API. */
  export const _internal = {
    get inflight() {
      return inflight
    },
  }
  // kilocode_change end

  export const DiffInput = z.object({
    sessionID: SessionID.zod,
    messageID: MessageID.zod.optional(),
  })

  export async function diff(input: z.infer<typeof DiffInput>) {
    return runPromise((svc) => svc.diff(input))
  }
}
