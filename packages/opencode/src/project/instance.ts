import { GlobalBus } from "@/bus/global"
import { disposeInstance } from "@/effect/instance-registry"
import { Filesystem } from "@/util/filesystem"
import { iife } from "@/util/iife"
import { Log } from "@/util/log"
import { Context } from "../util/context"
import { Project } from "./project"
import { State } from "./state"

export interface InstanceContext {
  directory: string
  worktree: string
  project: Project.Info
}

const context = Context.create<InstanceContext>("instance")
const cache = new Map<string, Promise<InstanceContext>>()

// kilocode_change start - idle eviction tracking
// Tracks last-use time and active request count per cached instance so
// the idle eviction sweeper can dispose instances that have been quiet
// without killing one that still has an in-flight request (e.g. a
// running session). Both maps are keyed by the resolved directory.
const lastUsed = new Map<string, number>()
const inflight = new Map<string, number>()
// kilocode_change end

const disposal = {
  all: undefined as Promise<void> | undefined,
}

function emit(directory: string) {
  GlobalBus.emit("event", {
    directory,
    payload: {
      type: "server.instance.disposed",
      properties: {
        directory,
      },
    },
  })
}

function boot(input: { directory: string; init?: () => Promise<any>; project?: Project.Info; worktree?: string }) {
  return iife(async () => {
    const ctx =
      input.project && input.worktree
        ? {
            directory: input.directory,
            worktree: input.worktree,
            project: input.project,
          }
        : await Project.fromDirectory(input.directory).then(({ project, sandbox }) => ({
            directory: input.directory,
            worktree: sandbox,
            project,
          }))
    await context.provide(ctx, async () => {
      await input.init?.()
    })
    return ctx
  })
}

function track(directory: string, next: Promise<InstanceContext>) {
  const task = next.catch((error) => {
    if (cache.get(directory) === task) cache.delete(directory)
    throw error
  })
  cache.set(directory, task)
  return task
}

export const Instance = {
  async provide<R>(input: { directory: string; init?: () => Promise<any>; fn: () => R }): Promise<R> {
    const directory = Filesystem.resolve(input.directory)
    let existing = cache.get(directory)
    if (!existing) {
      Log.Default.info("creating instance", { directory })
      existing = track(
        directory,
        boot({
          directory,
          init: input.init,
        }),
      )
    }
    // kilocode_change start - track in-flight requests and last-use time
    // so evictIdle() can dispose idle instances without racing live work.
    inflight.set(directory, (inflight.get(directory) ?? 0) + 1)
    lastUsed.set(directory, Date.now())
    try {
      const ctx = await existing
      return await context.provide(ctx, async () => {
        return input.fn()
      })
    } finally {
      inflight.set(directory, Math.max(0, (inflight.get(directory) ?? 1) - 1))
      lastUsed.set(directory, Date.now())
    }
    // kilocode_change end
  },
  get current() {
    return context.use()
  },
  get directory() {
    return context.use().directory
  },
  get worktree() {
    return context.use().worktree
  },
  get project() {
    return context.use().project
  },
  /**
   * Check if a path is within the project boundary.
   * Returns true if path is inside Instance.directory OR Instance.worktree.
   * Paths within the worktree but outside the working directory should not trigger external_directory permission.
   */
  containsPath(filepath: string) {
    if (Filesystem.contains(Instance.directory, filepath)) return true
    // Non-git projects set worktree to "/" which would match ANY absolute path.
    // Skip worktree check in this case to preserve external_directory permissions.
    if (Instance.worktree === "/") return false
    return Filesystem.contains(Instance.worktree, filepath)
  },
  /**
   * Captures the current instance ALS context and returns a wrapper that
   * restores it when called. Use this for callbacks that fire outside the
   * instance async context (native addons, event emitters, timers, etc.).
   */
  bind<F extends (...args: any[]) => any>(fn: F): F {
    const ctx = context.use()
    return ((...args: any[]) => context.provide(ctx, () => fn(...args))) as F
  },
  /**
   * Run a synchronous function within the given instance context ALS.
   * Use this to bridge from Effect (where InstanceRef carries context)
   * back to sync code that reads Instance.directory from ALS.
   */
  restore<R>(ctx: InstanceContext, fn: () => R): R {
    return context.provide(ctx, fn)
  },
  state<S>(init: () => S, dispose?: (state: Awaited<S>) => Promise<void>): () => S {
    return State.create(() => Instance.directory, init, dispose)
  },
  async reload(input: { directory: string; init?: () => Promise<any>; project?: Project.Info; worktree?: string }) {
    const directory = Filesystem.resolve(input.directory)
    Log.Default.info("reloading instance", { directory })
    await Promise.all([State.dispose(directory), disposeInstance(directory)])
    cache.delete(directory)
    lastUsed.delete(directory) // kilocode_change
    inflight.delete(directory) // kilocode_change
    const next = track(directory, boot({ ...input, directory }))
    emit(directory)
    return await next
  },
  async dispose() {
    const directory = Instance.directory
    Log.Default.info("disposing instance", { directory })
    await Promise.all([State.dispose(directory), disposeInstance(directory)])
    cache.delete(directory)
    lastUsed.delete(directory) // kilocode_change
    inflight.delete(directory) // kilocode_change
    emit(directory)
  },
  async disposeAll() {
    if (disposal.all) return disposal.all

    disposal.all = iife(async () => {
      Log.Default.info("disposing all instances")
      const entries = [...cache.entries()]
      for (const [key, value] of entries) {
        if (cache.get(key) !== value) continue

        const ctx = await value.catch((error) => {
          Log.Default.warn("instance dispose failed", { key, error })
          return undefined
        })

        if (!ctx) {
          if (cache.get(key) === value) cache.delete(key)
          continue
        }

        if (cache.get(key) !== value) continue

        await context.provide(ctx, async () => {
          await Instance.dispose()
        })
      }
    }).finally(() => {
      disposal.all = undefined
    })

    return disposal.all
  },
  // kilocode_change start - idle eviction
  /**
   * Dispose instances that haven't been used for `idleMs` and have no
   * in-flight requests. Releases file watchers, LSP, snapshot state,
   * DB handles, and PubSub queues so the native allocator can actually
   * return pages to the OS. The next request for that directory will
   * re-bootstrap from scratch.
   */
  async evictIdle(idleMs: number) {
    const cutoff = Date.now() - idleMs
    const stale: Array<[string, Promise<InstanceContext>]> = []
    for (const [dir, used] of lastUsed) {
      if (used >= cutoff) continue
      if ((inflight.get(dir) ?? 0) > 0) continue
      const entry = cache.get(dir)
      if (entry) stale.push([dir, entry])
    }
    for (const [dir, entry] of stale) {
      if (cache.get(dir) !== entry) continue
      if ((inflight.get(dir) ?? 0) > 0) continue
      const ctx = await entry.catch(() => undefined)
      if (!ctx) {
        if (cache.get(dir) === entry) cache.delete(dir)
        lastUsed.delete(dir)
        inflight.delete(dir)
        continue
      }
      Log.Default.info("evicting idle instance", { directory: dir, idleMs })
      await context.provide(ctx, async () => {
        await Instance.dispose()
      })
    }
    return stale.length
  },
  // kilocode_change end
}
