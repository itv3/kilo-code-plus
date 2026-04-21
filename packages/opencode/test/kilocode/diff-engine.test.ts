import { test, expect } from "bun:test"
import { DiffEngine } from "../../src/kilocode/snapshot/diff-engine"
import { Log } from "../../src/util/log"

Log.init({ print: false })

test("shouldSkip returns false for small inputs", () => {
  expect(DiffEngine.shouldSkip("a", "b")).toBe(false)
  expect(DiffEngine.shouldSkip("hello\nworld", "hello\nworld!")).toBe(false)
})

test("shouldSkip returns true when bytes exceed MAX_INPUT_BYTES", () => {
  const big = "x".repeat(DiffEngine.MAX_INPUT_BYTES + 1)
  expect(DiffEngine.shouldSkip(big, "small")).toBe(true)
  expect(DiffEngine.shouldSkip("small", big)).toBe(true)
})

test("shouldSkip returns true at exactly MAX_INPUT_LINES + 1", () => {
  const many = "a\n".repeat(DiffEngine.MAX_INPUT_LINES + 1)
  expect(DiffEngine.shouldSkip(many, "small")).toBe(true)
  expect(DiffEngine.shouldSkip("small", many)).toBe(true)
})
