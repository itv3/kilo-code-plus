// kilocode_change - new file
import path from "path"
import { describe, expect, test } from "bun:test"
import { ConfigProtection } from "../../../src/kilocode/permission/config-paths"
import { Global } from "../../../src/global"
import { KilocodePaths } from "../../../src/kilocode/paths"

describe("ConfigProtection.isRequest", () => {
  const config = path.resolve(Global.Path.config)
  const legacy = KilocodePaths.globalDirs().map((d) => path.resolve(d))

  test("returns false for external_directory targeting global config", () => {
    const result = ConfigProtection.isRequest({
      permission: "external_directory",
      patterns: [config + "/*"],
    })
    expect(result).toBe(false)
  })

  test("returns false for external_directory targeting skill dir", () => {
    const result = ConfigProtection.isRequest({
      permission: "external_directory",
      patterns: [path.join(config, "skills", "my-skill") + "/*"],
    })
    expect(result).toBe(false)
  })

  test("returns false for external_directory targeting legacy global dir", () => {
    for (const dir of legacy) {
      const result = ConfigProtection.isRequest({
        permission: "external_directory",
        patterns: [dir + "/*"],
      })
      expect(result).toBe(false)
    }
  })

  test("returns true for edit targeting global config file via metadata.filepath", () => {
    const result = ConfigProtection.isRequest({
      permission: "edit",
      patterns: [],
      metadata: { filepath: path.join(config, "config.json") },
    })
    expect(result).toBe(true)
  })

  test("returns true for edit targeting skill file via metadata.filepath", () => {
    const result = ConfigProtection.isRequest({
      permission: "edit",
      patterns: [],
      metadata: { filepath: path.join(config, "skills", "my-skill", "SKILL.md") },
    })
    expect(result).toBe(true)
  })

  test("returns true for edit targeting legacy global dir via metadata.filepath", () => {
    for (const dir of legacy) {
      const result = ConfigProtection.isRequest({
        permission: "edit",
        patterns: [],
        metadata: { filepath: path.join(dir, "config.json") },
      })
      expect(result).toBe(true)
    }
  })

  test("returns true for edit targeting relative config path via patterns", () => {
    const result = ConfigProtection.isRequest({
      permission: "edit",
      patterns: [".kilo/command/foo.md"],
    })
    expect(result).toBe(true)
  })

  test("returns false for edit targeting excluded subdir (plans)", () => {
    const result = ConfigProtection.isRequest({
      permission: "edit",
      patterns: [".kilo/plans/plan.md"],
    })
    expect(result).toBe(false)
  })

  test("returns false for read permission", () => {
    const result = ConfigProtection.isRequest({
      permission: "read",
      patterns: [".kilo/config.json"],
    })
    expect(result).toBe(false)
  })

  test("returns false for bash permission", () => {
    const result = ConfigProtection.isRequest({
      permission: "bash",
      patterns: ["cat " + path.join(config, "config.json")],
    })
    expect(result).toBe(false)
  })

  test("returns true for edit targeting root config files", () => {
    for (const file of ["kilo.json", "kilo.jsonc", "AGENTS.md"]) {
      const result = ConfigProtection.isRequest({
        permission: "edit",
        patterns: [file],
      })
      expect(result).toBe(true)
    }
  })

  test("returns false for edit targeting non-config files", () => {
    const result = ConfigProtection.isRequest({
      permission: "edit",
      patterns: ["src/index.ts"],
    })
    expect(result).toBe(false)
  })
})
