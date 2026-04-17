// kilocode_change - new file
//
// Dedicated worker thread for running the `diff` package's Myers algorithm
// (`structuredPatch` + `formatPatch`). These are O(N*M) and can easily block
// the main event loop for minutes on pathological inputs — which in the TUI
// is the same thread that serves the Hono API (ESC endpoint included) and
// the SSE heartbeats. Offloading them here keeps the main loop responsive.
//
// Protocol: parent posts { id, file, before, after, opts } and the worker
// responds with { id, patch } on success or { id, error } on failure. The
// worker never throws into the parent — every failure is surfaced via the
// `error` field so the parent can fall back to a sync path gracefully.

import { formatPatch, structuredPatch } from "diff"

type Req = {
  id: number
  file: string
  before: string
  after: string
  opts: { context?: number; ignoreWhitespace?: boolean }
}

type Res = { id: number; patch: string } | { id: number; error: string }

// Web Worker API (Bun workers). The parent runs this file via `new Worker(url)`.
self.onmessage = (evt: MessageEvent<Req>) => {
  const req = evt.data
  try {
    const ctx = req.opts.context ?? Number.MAX_SAFE_INTEGER
    const patch = formatPatch(
      structuredPatch(req.file, req.file, req.before, req.after, "", "", {
        context: Number.isFinite(ctx) ? ctx : Number.MAX_SAFE_INTEGER,
        ignoreWhitespace: req.opts.ignoreWhitespace,
      }),
    )
    self.postMessage({ id: req.id, patch } satisfies Res)
  } catch (err) {
    self.postMessage({
      id: req.id,
      error: err instanceof Error ? err.message : String(err),
    } satisfies Res)
  }
}
