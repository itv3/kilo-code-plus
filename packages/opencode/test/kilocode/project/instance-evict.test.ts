/**
 * Tests for Instance.evictIdle() — the idle eviction sweeper.
 *
 * PR #9046 disposed Instance contexts only when a worktree was deleted,
 * but long-lived Agent Manager sessions don't delete their worktrees.
 * Every active worktree's Instance held file watchers, LSP state,
 * snapshot handles, and PubSub queues forever — the main source of
 * native RSS growth observed in the kilo.DMP memory report.
 *
 * This test exercises the eviction path with short idle thresholds so
 * the cache can be verified to release entries and re-bootstrap on the
 * next request.
 */

import { describe, test, expect } from "bun:test"
import { Instance } from "../../../src/project/instance"
import { tmpdir } from "../../fixture/fixture"

describe("Instance.evictIdle", () => {
  test("disposes instances older than the cutoff and preserves fresh ones", async () => {
    await using a = await tmpdir({ git: true })
    await using b = await tmpdir({ git: true })

    const inits: string[] = []
    const bootA = () => {
      inits.push("a")
      return Promise.resolve()
    }
    const bootB = () => {
      inits.push("b")
      return Promise.resolve()
    }

    await Instance.provide({ directory: a.path, init: bootA, fn: async () => undefined })
    await Instance.provide({ directory: b.path, init: bootB, fn: async () => undefined })
    expect(inits).toEqual(["a", "b"])

    // Let a become idle while b is kept warm with a second touch.
    await new Promise((r) => setTimeout(r, 50))
    await Instance.provide({ directory: b.path, init: bootB, fn: async () => undefined })

    // Threshold sits between the two lastUsed timestamps so only a is evicted.
    const evicted = await Instance.evictIdle(30)
    expect(evicted).toBeGreaterThanOrEqual(1)

    // a is gone — next provide re-runs init. b is cached — init does not run.
    await Instance.provide({ directory: a.path, init: bootA, fn: async () => undefined })
    await Instance.provide({ directory: b.path, init: bootB, fn: async () => undefined })
    expect(inits.filter((x) => x === "a")).toHaveLength(2)
    expect(inits.filter((x) => x === "b")).toHaveLength(1)

    await Instance.disposeAll()
  }, 30_000)

  test("skips instances with in-flight requests regardless of age", async () => {
    await using t = await tmpdir({ git: true })

    // Hold provide() open so the in-flight counter stays > 0.
    const release = Promise.withResolvers<void>()
    const running = Instance.provide({
      directory: t.path,
      fn: async () => {
        await release.promise
      },
    })

    // Give the request a moment to register then evict with a zero cutoff
    // which would evict every cache entry if in-flight were ignored.
    await new Promise((r) => setTimeout(r, 50))
    await Instance.evictIdle(0)

    release.resolve()
    await running

    // The cache entry survived — provide() with no init callback returns
    // without re-bootstrapping. We verify by measuring that a brand new
    // init callback is NOT invoked.
    const inits: string[] = []
    await Instance.provide({
      directory: t.path,
      init: () => {
        inits.push("x")
        return Promise.resolve()
      },
      fn: async () => undefined,
    })
    expect(inits).toHaveLength(0)

    await Instance.disposeAll()
  }, 30_000)

  test("returns 0 when nothing is idle", async () => {
    await using t = await tmpdir({ git: true })
    await Instance.provide({ directory: t.path, fn: async () => undefined })

    const evicted = await Instance.evictIdle(60_000)
    expect(evicted).toBe(0)

    await Instance.disposeAll()
  }, 30_000)
})
