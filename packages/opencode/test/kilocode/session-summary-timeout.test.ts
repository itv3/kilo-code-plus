// kilocode_change - new file
// Tests the tracked dispatcher that wraps SessionSummary.summarize:
//   - cancel(sessionID) drops the inflight entry so ESC can stop a long summary.
//   - back-to-back summarize calls for the same session replace the previous one.
//   - cancel() on an unknown sessionID is safe (no throw).

import { test, expect, afterAll } from "bun:test"
import { $ } from "bun"
import { Cause, Effect } from "effect"
import { Instance } from "../../src/project/instance"
import { Session } from "../../src/session"
import { SessionSummary } from "../../src/session/summary"
import { DiffEngine } from "../../src/kilocode/snapshot/diff-engine"
import { Log } from "../../src/util/log"
import { tmpdir } from "../fixture/fixture"
import type { SessionID } from "../../src/session/schema"

Log.init({ print: false })

afterAll(async () => {
  await DiffEngine.shutdown()
})

async function bootstrap() {
  return tmpdir({
    git: true,
    init: async (dir) => {
      await $`git commit --allow-empty --no-gpg-sign -m "init"`.cwd(dir).quiet()
    },
  })
}

test("cancel on an unknown sessionID is a no-op (does not throw)", async () => {
  // Explicit sanity check — the ESC path calls this blind.
  const fake = "ses_does_not_exist" as SessionID
  await SessionSummary.cancel(fake)
  expect(SessionSummary._internal.inflight.has(fake)).toBe(false)
})

test("cancel aborts an inflight summary entry", async () => {
  await using tmp = await bootstrap()
  await Instance.provide({
    directory: tmp.path,
    fn: async () => {
      const session = await Session.create({})

      // Install a synthetic inflight entry (we can't easily force summarize to
      // be slow without its real dependencies — but we CAN prove the cancel
      // path works by seeding state and observing it drain).
      const ac = new AbortController()
      SessionSummary._internal.inflight.set(session.id, ac)
      expect(SessionSummary._internal.inflight.has(session.id)).toBe(true)

      await SessionSummary.cancel(session.id)

      expect(ac.signal.aborted).toBe(true)
      expect(SessionSummary._internal.inflight.has(session.id)).toBe(false)
    },
  })
})

test("summarize clears inflight entry after completion on an empty session", async () => {
  // With no messages, summarize returns immediately. The dispatcher must still
  // clean up the inflight entry. Regression guard for the ensuring() hook.
  await using tmp = await bootstrap()
  await Instance.provide({
    directory: tmp.path,
    fn: async () => {
      const session = await Session.create({})
      SessionSummary.summarize({ sessionID: session.id, messageID: "msg_noop" as any })
      // Give it a few ticks to finish — summarize is fire-and-forget.
      for (let i = 0; i < 50; i++) {
        if (!SessionSummary._internal.inflight.has(session.id)) break
        await Bun.sleep(20)
      }
      expect(SessionSummary._internal.inflight.has(session.id)).toBe(false)
    },
  })
})

test("starting a second summarize for the same session aborts the first", async () => {
  await using tmp = await bootstrap()
  await Instance.provide({
    directory: tmp.path,
    fn: async () => {
      const session = await Session.create({})

      // Seed a manual inflight entry — simulate a long-running summary that
      // hasn't finished yet. The dispatcher should abort it when we call
      // summarize again for the same sessionID.
      const first = new AbortController()
      SessionSummary._internal.inflight.set(session.id, first)

      SessionSummary.summarize({ sessionID: session.id, messageID: "msg_noop" as any })

      expect(first.signal.aborted).toBe(true)
      // The map entry has been replaced with the new call's controller (or
      // cleared after completion). Either way the first controller is gone.
      expect(SessionSummary._internal.inflight.get(session.id)).not.toBe(first)
    },
  })
})

test("AbortSignal-to-Effect.interrupt bridge fires fast", async () => {
  // Mirrors the bridge used inside SessionSummary.summarize: a racer that
  // calls `resume(Effect.interrupt)` when an AbortController is aborted.
  // Proves the hang-in-race will interrupt promptly.
  //
  // Uses `raceAllFirst` (not `raceAll`) because `raceAll` waits for the first
  // SUCCESS and would ignore an interrupt-completing racer — exactly the bug
  // this test exists to regression-guard against.
  const ac = new AbortController()
  const start = Date.now()
  setTimeout(() => ac.abort(), 25)

  const exit = await Effect.runPromiseExit(
    Effect.raceAllFirst([
      Effect.sleep("60 seconds").pipe(Effect.andThen(Effect.succeed("never" as string))),
      Effect.callback<string>((resume) => {
        if (ac.signal.aborted) {
          resume(Effect.interrupt)
          return
        }
        const onAbort = () => resume(Effect.interrupt)
        ac.signal.addEventListener("abort", onAbort, { once: true })
        return Effect.sync(() => ac.signal.removeEventListener("abort", onAbort))
      }),
    ]),
  )
  const elapsed = Date.now() - start

  // Without the bridge, the race would wait 60s. With it, it unwinds in a
  // handful of ms after the setTimeout fires.
  expect(elapsed).toBeLessThan(500)
  expect(exit._tag).toBe("Failure")
  if (exit._tag === "Failure") {
    expect(Cause.hasInterrupts(exit.cause)).toBe(true)
  }
})

test("catchCause skips interrupt causes (no double warning)", async () => {
  // Regression: `Effect.catchCause` catches interrupt causes by default. We
  // guard with `Cause.hasInterrupts(cause)` so only real failures reach the
  // "summary_failed" publish path. This test exercises the guard directly.
  let interruptHandlerRan = false
  let catchCauseHandlerRan = false

  const program = Effect.raceAllFirst([
    Effect.sleep("60 seconds").pipe(Effect.andThen(Effect.succeed(undefined))),
    Effect.sleep("10 millis").pipe(Effect.andThen(Effect.interrupt)),
  ]).pipe(
    Effect.onInterrupt(() =>
      Effect.sync(() => {
        interruptHandlerRan = true
      }),
    ),
    Effect.catchCause((cause) =>
      Effect.sync(() => {
        if (Cause.hasInterrupts(cause)) return
        catchCauseHandlerRan = true
      }),
    ),
  )

  await Effect.runPromise(program)
  expect(interruptHandlerRan).toBe(true)
  expect(catchCauseHandlerRan).toBe(false)
})
