import { afterEach, describe, expect, test } from "bun:test"
import path from "node:path"
import { tmpdir } from "../../fixture/fixture"
import { Instruction } from "../../../src/session/instruction"
import { Instance } from "../../../src/project/instance"
import { MessageID } from "../../../src/session/schema"
import { Filesystem } from "../../../src/util/filesystem"

afterEach(async () => {
  delete process.env.KILO_INSTRUCTION_TEST
  await Instance.disposeAll()
})

describe("instruction markdown substitutions", () => {
  test("applies file and env substitutions to nearby AGENTS.md", async () => {
    process.env.KILO_INSTRUCTION_TEST = "env content"
    await using tmp = await tmpdir({
      init: async (dir) => {
        await Filesystem.write(path.join(dir, "subdir", "guide.md"), "file content")
        await Filesystem.write(
          path.join(dir, "subdir", "AGENTS.md"),
          ["# Instructions", "", "{file:guide.md}", "{env:KILO_INSTRUCTION_TEST}"].join("\n"),
        )
        await Filesystem.write(path.join(dir, "subdir", "nested", "file.ts"), "const value = 1")
      },
    })

    await Instance.provide({
      directory: tmp.path,
      fn: async () => {
        const results = await Instruction.resolve(
          [],
          path.join(tmp.path, "subdir", "nested", "file.ts"),
          MessageID.make("message-instruction-substitution"),
        )

        expect(results).toHaveLength(1)
        expect(results[0].content).toContain("file content")
        expect(results[0].content).toContain("env content")
        expect(results[0].content).not.toContain("{file:")
        expect(results[0].content).not.toContain("{env:")
      },
    })
  })
})
