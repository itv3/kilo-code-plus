// kilocode_change - fallback-only cap around the JS Myers path.
//
// Primary patch generation goes through `DiffFull.batch` / `DiffFull.file`
// (git-based). This module exists solely so that if git fails and we fall
// back to `structuredPatch`, we don't reintroduce the event-loop freeze on
// huge-file diffs. Never hit in normal operation.

export namespace DiffEngine {
  /** Hard byte cap on a single side (before or after) of a diff. 512 KB. */
  export const MAX_INPUT_BYTES = 512 * 1024
  /** Hard line cap on a single side of a diff. */
  export const MAX_INPUT_LINES = 2000

  function lines(text: string) {
    if (!text) return 0
    const len = text.length
    if (len === 0) return 0
    let count = 1
    for (let i = 0; i < len; i++) {
      if (text.charCodeAt(i) === 10) count++
    }
    // trailing newline does not create an extra line
    if (text.charCodeAt(len - 1) === 10) count--
    return count
  }

  /** Returns true if the inputs are too big to run through `structuredPatch` safely. */
  export function shouldSkip(before: string, after: string): boolean {
    if (before.length > MAX_INPUT_BYTES || after.length > MAX_INPUT_BYTES) return true
    if (lines(before) > MAX_INPUT_LINES || lines(after) > MAX_INPUT_LINES) return true
    return false
  }
}
