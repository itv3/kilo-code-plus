import { Bus } from "../bus"
import { BusEvent } from "../bus/bus-event"
import { Identifier } from "../id/id"
import { Instance } from "../project/instance"
import { Log } from "../util/log"
import { fn } from "../util/fn"
import { MCP } from "../mcp"
import z from "zod"

export namespace SessionNetwork {
  const log = Log.create({ service: "session.network" })
  const codes = new Set(["ECONNRESET", "ECONNREFUSED", "ENOTFOUND", "EAI_AGAIN", "ETIMEDOUT", "ENETUNREACH"])

  export const Wait = z
    .object({
      id: Identifier.schema("question"),
      sessionID: Identifier.schema("session"),
      message: z.string(),
      time: z.object({
        created: z.number(),
      }),
    })
    .meta({
      ref: "SessionNetworkWait",
    })
  export type Wait = z.infer<typeof Wait>

  export const Event = {
    Asked: BusEvent.define("session.network.asked", Wait),
    Replied: BusEvent.define(
      "session.network.replied",
      z.object({
        sessionID: z.string(),
        requestID: z.string(),
      }),
    ),
    Rejected: BusEvent.define(
      "session.network.rejected",
      z.object({
        sessionID: z.string(),
        requestID: z.string(),
      }),
    ),
  }

  const state = Instance.state(async () => {
    const pending: Record<
      string,
      {
        info: Wait
        resolve: () => void
        reject: (e: unknown) => void
      }
    > = {}
    return { pending }
  })

  export function code(err: unknown) {
    const code = (err as { code?: unknown })?.code
    return typeof code === "string" ? code : undefined
  }

  export function disconnected(err: unknown) {
    const match = code(err)
    if (match && codes.has(match)) return true
    const msg = err instanceof Error ? err.message : String(err)
    if (msg.includes("fetch failed")) return true
    if (msg.includes("network is unreachable")) return true
    if (msg.includes("socket connection")) return true
    return false
  }

  export function message(err: unknown) {
    const match = code(err)
    if (match === "ECONNRESET") return "Connection reset by server"
    if (match === "ECONNREFUSED") return "Connection refused"
    if (match === "ENOTFOUND") return "Host not found"
    if (match === "EAI_AGAIN") return "DNS lookup failed"
    if (match === "ETIMEDOUT") return "Connection timed out"
    if (match === "ENETUNREACH") return "Network is unreachable"
    const msg = err instanceof Error ? err.message : String(err)
    if (msg.includes("fetch failed")) return "Network request failed"
    return "Network connection failed"
  }

  export async function ask(input: { sessionID: string; message: string; abort: AbortSignal }) {
    const s = await state()
    const id = Identifier.ascending("question")
    const info: Wait = {
      id,
      sessionID: input.sessionID,
      message: input.message,
      time: {
        created: Date.now(),
      },
    }

    return new Promise<void>((resolve, reject) => {
      const onAbort = () => {
        if (!s.pending[id]) return
        input.abort.removeEventListener("abort", onAbort)
        delete s.pending[id]
        Bus.publish(Event.Rejected, {
          sessionID: input.sessionID,
          requestID: id,
        })
        reject(new DOMException("Aborted", "AbortError"))
      }
      s.pending[id] = {
        info,
        resolve: () => {
          input.abort.removeEventListener("abort", onAbort)
          resolve()
        },
        reject: (err) => {
          input.abort.removeEventListener("abort", onAbort)
          reject(err)
        },
      }
      input.abort.addEventListener("abort", onAbort, { once: true })
      if (input.abort.aborted) {
        onAbort()
        return
      }
      Bus.publish(Event.Asked, info)
    })
  }

  export const reply = fn(
    z.object({
      requestID: z.string(),
    }),
    async (input) => {
      const s = await state()
      const req = s.pending[input.requestID]
      if (!req) {
        log.warn("reply for unknown request", { requestID: input.requestID })
        return
      }
      delete s.pending[input.requestID]
      await MCP.reconnectRemote().catch((err) => {
        log.error("remote reconnect failed", { err })
      })
      Bus.publish(Event.Replied, {
        sessionID: req.info.sessionID,
        requestID: req.info.id,
      })
      req.resolve()
    },
  )

  export const reject = fn(
    z.object({
      requestID: z.string(),
    }),
    async (input) => {
      const s = await state()
      const req = s.pending[input.requestID]
      if (!req) {
        log.warn("reject for unknown request", { requestID: input.requestID })
        return
      }
      delete s.pending[input.requestID]
      Bus.publish(Event.Rejected, {
        sessionID: req.info.sessionID,
        requestID: req.info.id,
      })
      req.reject(new RejectedError())
    },
  )

  export async function list() {
    return state().then((s) => Object.values(s.pending).map((item) => item.info))
  }

  export class RejectedError extends Error {
    constructor() {
      super("Network reconnect was rejected")
    }
  }
}
