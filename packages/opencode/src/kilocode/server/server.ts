// kilocode_change - new file
// Kilo-specific overrides for the server control plane.
// Imported by ../../server/server.ts with minimal kilocode_change markers.

import { ModelCache } from "../../provider/model-cache"
import { Instance } from "../../project/instance"
import { Log } from "../../util/log"

/** Extra paths to skip request logging for */
export function skipLogging(path: string): boolean {
  return path === "/telemetry/capture" || path === "/global/health"
}

/** Additional CORS origin check for *.kilo.ai */
export function corsOrigin(input: string): string | undefined {
  if (/^https:\/\/([a-z0-9-]+\.)*kilo\.ai$/.test(input)) {
    return input
  }
  return undefined
}

/** Invalidate model cache and provider state after auth change */
export async function authChanged(providerID: string) {
  ModelCache.clear(providerID)
  await Instance.disposeAll()
}

export const DOC_TITLE = "kilo"
export const DOC_DESCRIPTION = "kilo api"

// ---------------------------------------------------------------------------
// Idle instance eviction
// ---------------------------------------------------------------------------
// VS Code Agent Manager leaves one Instance alive per worktree for the whole
// session. Each Instance holds file watchers, LSP state, snapshot gitdir
// handles, DB connections, and PubSub queues. Without eviction these
// accumulate until the user closes VS Code — which is the main source of
// the multi-GB "kilo serve" RSS growth observed on Windows.
//
// The sweeper disposes any instance that hasn't served a request for
// IDLE_MS and has no in-flight work. The next request for that directory
// re-bootstraps from fresh state.

const log = Log.create({ service: "instance-evictor" })
const IDLE_MS = 10 * 60 * 1000 // 10 minutes
const SWEEP_MS = 60 * 1000 // check every minute

const evictor = { timer: undefined as ReturnType<typeof setInterval> | undefined }

export function startIdleEviction() {
  if (evictor.timer) return
  evictor.timer = setInterval(() => {
    Instance.evictIdle(IDLE_MS).catch((err) => {
      log.error("evictIdle failed", { error: err instanceof Error ? err.message : String(err) })
    })
  }, SWEEP_MS)
  evictor.timer.unref?.()
  log.info("idle eviction started", { idleMs: IDLE_MS, sweepMs: SWEEP_MS })
}

export function stopIdleEviction() {
  if (!evictor.timer) return
  clearInterval(evictor.timer)
  evictor.timer = undefined
}
