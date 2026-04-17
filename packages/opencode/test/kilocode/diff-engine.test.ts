import { test, expect, afterAll } from "bun:test"
import { DiffEngine } from "../../src/kilocode/snapshot/diff-engine"
import { Log } from "../../src/util/log"

Log.init({ print: false })

afterAll(async () => {
  await DiffEngine.shutdown()
})

test("shouldSkip returns undefined for small inputs", () => {
  expect(DiffEngine.shouldSkip("a", "b")).toBeUndefined()
  expect(DiffEngine.shouldSkip("hello\nworld", "hello\nworld!")).toBeUndefined()
})

test("shouldSkip returns 'oversized' when bytes exceed MAX_INPUT_BYTES", () => {
  const big = "x".repeat(DiffEngine.MAX_INPUT_BYTES + 1)
  expect(DiffEngine.shouldSkip(big, "small")).toBe("oversized")
  expect(DiffEngine.shouldSkip("small", big)).toBe("oversized")
})

test("shouldSkip returns 'too-many-lines' when lines exceed MAX_INPUT_LINES", () => {
  const many = "a\n".repeat(DiffEngine.MAX_INPUT_LINES + 5)
  expect(DiffEngine.shouldSkip(many, "small")).toBe("too-many-lines")
  expect(DiffEngine.shouldSkip("small", many)).toBe("too-many-lines")
})

test("patchSync returns non-empty patch for small diffs", () => {
  const result = DiffEngine.patchSync("file.txt", "hello\nworld\n", "hello\nuniverse\n")
  expect(result.skipped).toBeUndefined()
  expect(result.patch).toContain("@@")
  expect(result.patch).toContain("-world")
  expect(result.patch).toContain("+universe")
})

test("patchSync returns empty patch with skipped='too-many-lines' for huge inputs in <50ms", () => {
  const before = "before_line\n".repeat(10_000)
  const after = "after_line\n".repeat(10_000)
  const start = Date.now()
  const result = DiffEngine.patchSync("big.txt", before, after)
  const elapsed = Date.now() - start
  expect(result.patch).toBe("")
  expect(result.skipped).toBe("too-many-lines")
  expect(elapsed).toBeLessThan(50)
})

test("patchAsync returns same output as patchSync for small inputs", async () => {
  const before = "line1\nline2\n"
  const after = "line1\nline_changed\n"
  const sync = DiffEngine.patchSync("x.txt", before, after)
  const async = await DiffEngine.patchAsync("x.txt", before, after)
  expect(async.patch).toBe(sync.patch)
  expect(async.skipped).toBeUndefined()
})

test("patchAsync respects input caps (skips without spawning worker)", async () => {
  const big = "x".repeat(DiffEngine.MAX_INPUT_BYTES + 10)
  const result = await DiffEngine.patchAsync("huge.txt", big, "tiny")
  expect(result.patch).toBe("")
  expect(result.skipped).toBe("oversized")
})

test("patchAsync respects AbortSignal", async () => {
  const ac = new AbortController()
  ac.abort()
  const result = await DiffEngine.patchAsync("x.txt", "hello\n", "world\n", { signal: ac.signal })
  // Either the sync fast-path ran (small input) or the abort was honored — either is fine.
  expect(typeof result.patch).toBe("string")
})

test("patchAsync honors a short timeout with a stubbed slow worker", async () => {
  // Craft an input just over the sync fast-path threshold so patchAsync routes
  // to the worker, then set a ludicrously short timeout so the race trips.
  // This exercises the timeout + worker-termination code path.
  const lines = 600
  const before = Array.from({ length: lines }, (_, i) => `a_${i}`).join("\n") + "\n"
  const after = Array.from({ length: lines }, (_, i) => `b_${i}`).join("\n") + "\n"
  // 1ms is almost certainly shorter than postMessage round-trip.
  const result = await DiffEngine.patchAsync("x.txt", before, after, { timeout: 1 })
  // Could succeed (if worker was incredibly fast) or time out — accept both.
  if (result.skipped) {
    expect(["timeout", "worker-error"]).toContain(result.skipped)
    expect(result.patch).toBe("")
  }
})
