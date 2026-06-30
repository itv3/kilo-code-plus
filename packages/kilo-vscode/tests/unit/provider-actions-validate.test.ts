import { describe, expect, it } from "bun:test"
import { DEFAULT_FAVORITES, validateModelSelections, validateRecents, validateFavorites } from "../../src/provider-actions"

describe("validateModelSelections", () => {
  it("returns empty object for null", () => {
    expect(validateModelSelections(null)).toEqual({})
  })

  it("returns empty object for undefined", () => {
    expect(validateModelSelections(undefined)).toEqual({})
  })

  it("returns empty object for array", () => {
    expect(validateModelSelections([{ providerID: "a", modelID: "b" }])).toEqual({})
  })

  it("returns empty object for non-object primitives", () => {
    expect(validateModelSelections("string")).toEqual({})
    expect(validateModelSelections(42)).toEqual({})
    expect(validateModelSelections(true)).toEqual({})
  })

  it("passes through valid selections", () => {
    const input = {
      code: { providerID: "anthropic", modelID: "claude-sonnet-4" },
      ask: { providerID: "openai", modelID: "gpt-4.1" },
    }
    expect(validateModelSelections(input)).toEqual(input)
  })

  it("filters out entries with non-string providerID", () => {
    const input = {
      code: { providerID: "anthropic", modelID: "claude-sonnet-4" },
      broken: { providerID: 42, modelID: "model" },
    }
    expect(validateModelSelections(input)).toEqual({
      code: { providerID: "anthropic", modelID: "claude-sonnet-4" },
    })
  })

  it("filters out entries with non-string modelID", () => {
    const input = {
      code: { providerID: "anthropic", modelID: "claude-sonnet-4" },
      broken: { providerID: "openai", modelID: null },
    }
    expect(validateModelSelections(input)).toEqual({
      code: { providerID: "anthropic", modelID: "claude-sonnet-4" },
    })
  })

  it("filters out null and non-object entries", () => {
    const input = {
      code: { providerID: "anthropic", modelID: "claude-sonnet-4" },
      empty: null,
      str: "not-an-object",
      num: 123,
    }
    expect(validateModelSelections(input)).toEqual({
      code: { providerID: "anthropic", modelID: "claude-sonnet-4" },
    })
  })

  it("strips extra properties from valid entries", () => {
    const input = {
      code: { providerID: "anthropic", modelID: "claude-sonnet-4", extra: true, nested: { x: 1 } },
    }
    const result = validateModelSelections(input)
    expect(result).toEqual({ code: { providerID: "anthropic", modelID: "claude-sonnet-4" } })
    expect(Object.keys(result.code!)).toEqual(["providerID", "modelID"])
  })

  it("returns empty object for empty input object", () => {
    expect(validateModelSelections({})).toEqual({})
  })
})

describe("validateFavorites", () => {
  it("returns the default favorite models before favorites are initialized", () => {
    expect(validateFavorites(undefined)).toEqual(DEFAULT_FAVORITES)
  })

  it("does not restore defaults after favorites are explicitly cleared", () => {
    expect(validateFavorites([])).toEqual([])
  })

  it("returns an empty list for invalid persisted data", () => {
    expect(validateFavorites(null)).toEqual([])
    expect(validateFavorites("invalid")).toEqual([])
  })

  it("filters and sanitizes persisted favorites", () => {
    const input = [
      { providerID: "kilo", modelID: "stepfun/step-3.7-flash:free", extra: true },
      { providerID: 42, modelID: "broken" },
      { providerID: "openai", modelID: null },
    ]
    expect(validateFavorites(input)).toEqual([{ providerID: "kilo", modelID: "stepfun/step-3.7-flash:free" }])
  })
})
