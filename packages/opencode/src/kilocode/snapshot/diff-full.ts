// kilocode_change - new file
//
// Extracted from `Snapshot.diffFull` to keep our diff against the shared
// `src/snapshot/index.ts` as small as possible. This is the per-row diff
// loop that:
//
//   - Runs the npm `diff` package inside a worker (with caps and a timeout)
//     via `DiffEngine.patchAsync`, so a file with tens of thousands of lines
//     cannot block the event loop.
//   - Tracks `skipped` files and publishes a single warning after the loop.
//   - Yields to the Effect scheduler between files so abort + heartbeat
//     endpoints stay responsive during a large diff.
//
// The helper returns its own accumulator; the caller passes it the rows and
// the two closed-over readers (`load`, `show`) from the snapshot layer.

import { Effect } from "effect"
import type { Bus } from "@/bus"
import { Log } from "@/util/log"
import { SessionWarningEvent } from "../session/warning"
import { DiffEngine } from "./diff-engine"

export namespace DiffFull {
  const log = Log.create({ service: "snapshot.diff-full" })

  export interface Row {
    file: string
    status: "added" | "deleted" | "modified"
    binary: boolean
    additions: number
    deletions: number
  }

  export interface Result {
    file: string
    patch: string
    additions: number
    deletions: number
    status: "added" | "deleted" | "modified"
  }

  export function run<E1, E2, R1, R2>(params: {
    rows: Row[]
    step: number
    from: string
    to: string
    bus: Bus.Interface
    load: (run: Row[]) => Effect.Effect<Map<string, { before: string; after: string }> | undefined, E1, R1>
    show: (row: Row) => Effect.Effect<string[], E2, R2>
  }) {
    return Effect.gen(function* () {
      const timer = log.time("diffFull", { from: params.from, to: params.to, rows: params.rows.length })
      const out: Result[] = []
      const skipped: { file: string; reason: DiffEngine.SkipReason }[] = []

      for (let i = 0; i < params.rows.length; i += params.step) {
        const batch = params.rows.slice(i, i + params.step)
        const text = yield* params.load(batch)

        for (const row of batch) {
          const hit = text?.get(row.file) ?? { before: "", after: "" }
          const pair = row.binary ? ["", ""] : text ? [hit.before, hit.after] : yield* params.show(row)
          const before = pair[0] ?? ""
          const after = pair[1] ?? ""
          const res = row.binary
            ? { patch: "" as string, skipped: undefined as DiffEngine.SkipReason | undefined }
            : yield* Effect.promise(() => DiffEngine.patchAsync(row.file, before, after))
          if (res.skipped) {
            skipped.push({ file: row.file, reason: res.skipped })
            log.warn("diffFull.skipped", {
              file: row.file,
              reason: res.skipped,
              bytesBefore: before.length,
              bytesAfter: after.length,
            })
          }
          out.push({
            file: row.file,
            patch: res.patch,
            additions: row.additions,
            deletions: row.deletions,
            status: row.status,
          })
          // Let the runtime scheduler yield between files so the event loop
          // can service the abort endpoint and SSE heartbeat.
          yield* Effect.yieldNow
        }
      }

      if (skipped.length > 0) {
        yield* params.bus
          .publish(SessionWarningEvent, {
            sessionID: undefined,
            kind: "diff_skipped",
            message:
              skipped.length === 1
                ? `Skipped diff for ${skipped[0]!.file} (${skipped[0]!.reason})`
                : `Skipped diffs for ${skipped.length} files`,
            details: { files: skipped },
          })
          .pipe(Effect.ignore)
      }

      timer.stop()
      return out
    })
  }
}
