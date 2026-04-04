import { describe, expect, test } from "bun:test"
import path from "node:path"
import fs from "node:fs/promises"
import { tmpdir } from "../fixture/fixture"
import { ConfigPaths } from "../../src/config/paths"

describe("ConfigPaths.substitute", () => {
  describe("escapeJson parameter", () => {
    test("escapes newlines when escapeJson is true (default)", async () => {
      await using tmp = await tmpdir({
        git: true,
        init: async (dir) => {
          await Bun.write(path.join(dir, "test.txt"), "Line 1\nLine 2\nLine 3")
        },
      })

      const result = await ConfigPaths.substitute(
        "{file:./test.txt}",
        { dir: tmp.path, source: "config.json" },
        "error",
        true,
      )

      expect(result).toBe("Line 1\\nLine 2\\nLine 3")
    })

    test("preserves newlines when escapeJson is false", async () => {
      await using tmp = await tmpdir({
        git: true,
        init: async (dir) => {
          await Bun.write(path.join(dir, "test.txt"), "Line 1\nLine 2\nLine 3")
        },
      })

      const result = await ConfigPaths.substitute(
        "{file:./test.txt}",
        { dir: tmp.path, source: "agent.md" },
        "empty",
        false,
      )

      expect(result).toBe("Line 1\nLine 2\nLine 3")
    })

    test("escapes quotes when escapeJson is true", async () => {
      await using tmp = await tmpdir({
        git: true,
        init: async (dir) => {
          await Bun.write(path.join(dir, "test.txt"), 'Text with "quotes"')
        },
      })

      const result = await ConfigPaths.substitute(
        "{file:./test.txt}",
        { dir: tmp.path, source: "config.json" },
        "error",
        true,
      )

      expect(result).toBe('Text with \\"quotes\\"')
    })

    test("preserves quotes when escapeJson is false", async () => {
      await using tmp = await tmpdir({
        git: true,
        init: async (dir) => {
          await Bun.write(path.join(dir, "test.txt"), 'Text with "quotes"')
        },
      })

      const result = await ConfigPaths.substitute(
        "{file:./test.txt}",
        { dir: tmp.path, source: "agent.md" },
        "empty",
        false,
      )

      expect(result).toBe('Text with "quotes"')
    })
  })

  describe("existing functionality", () => {
    test("substitutes {env:VAR} with environment variable", async () => {
      process.env.TEST_VAR = "test_value"

      const result = await ConfigPaths.substitute("Value: {env:TEST_VAR}", { dir: "/tmp", source: "test.json" })

      expect(result).toBe("Value: test_value")

      delete process.env.TEST_VAR
    })

    test("replaces missing {env:VAR} with empty string", async () => {
      const result = await ConfigPaths.substitute("Value: {env:NONEXISTENT_VAR}", { dir: "/tmp", source: "test.json" })

      expect(result).toBe("Value: ")
    })
  })
})
