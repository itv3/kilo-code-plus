/**
 * Tests for the single-allocation git() output helper.
 *
 * Where the old helper copied every pipe chunk into a Buffer and then
 * concat+toString'd them, the new helper references the chunks directly
 * and collapses to one Uint8Array + one decode at the end. This catches
 * regressions that would re-introduce the per-chunk Buffer.from/concat
 * pattern (the actual source of mimalloc arena retention in the PR #9046
 * memory report).
 */

import { describe, test, expect } from "bun:test"
import fs from "node:fs"
import path from "node:path"

const SRC = path.resolve(import.meta.dir, "../../../src/kilocode/review/worktree-diff.ts")

describe("worktree-diff stream helper", () => {
  const src = fs.readFileSync(SRC, "utf-8")

  test("no per-chunk Buffer.from copy", () => {
    // The old helper did `chunks.push(Buffer.from(value))` which allocates
    // once per chunk. Each small allocation is what mimalloc retains
    // forever in its arenas.
    expect(src).not.toContain("Buffer.from(value)")
  })

  test("no Buffer.concat in git helper", () => {
    const start = src.indexOf("async function git(")
    expect(start).toBeGreaterThan(-1)
    const end = src.indexOf("\nfunction ", start)
    const body = src.slice(start, end > 0 ? end : start + 2000)
    expect(body).not.toContain("Buffer.concat")
  })

  test("chunks are Uint8Array, not Buffer", () => {
    const start = src.indexOf("async function git(")
    const body = src.slice(start, start + 1200)
    expect(body).toContain("Uint8Array[]")
    expect(body).not.toContain("chunks: Buffer[]")
  })

  test("decoder is reused across calls", () => {
    expect(src).toContain("const decoder = new TextDecoder()")
    // The only decode() call should go through the shared instance.
    const inlineDecoder = src.match(/new TextDecoder\(\)\.decode/g)
    expect(inlineDecoder ?? []).toHaveLength(0)
  })

  test("bounded fast path when output fits in one chunk", () => {
    const joinStart = src.indexOf("function join(")
    expect(joinStart).toBeGreaterThan(-1)
    const body = src.slice(joinStart, joinStart + 400)
    // Single-chunk fast path avoids even the intermediate buffer.
    expect(body).toContain("chunks.length === 1")
  })
})
