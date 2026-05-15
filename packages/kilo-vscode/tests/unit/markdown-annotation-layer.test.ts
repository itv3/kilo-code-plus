import { describe, expect, it } from "bun:test"
import { readFileSync } from "node:fs"
import { join } from "node:path"

const file = join(import.meta.dir, "../../webview-ui/agent-manager/MarkdownAnnotationLayer.tsx")
const src = readFileSync(file, "utf8")

describe("MarkdownAnnotationLayer", () => {
  it("ignores inline annotation DOM mutations while watching rendered Markdown", () => {
    expect(src).toMatch(/function isAnnotationMutation[\s\S]*target\.closest\(selector\)/)
    expect(src).toMatch(
      /new MutationObserver\(\(mutations\) => \{[\s\S]*mutations\.every\(isAnnotationMutation\)[\s\S]*schedule\(\)/,
    )
  })
})
