export namespace Rpc {
  type Definition = {
    [method: string]: (input: any) => any
  }

  // kilocode_change start — support both Worker (postMessage) and subprocess (process.send) IPC.
  // Auto-detect at module load: if process.send exists we are a Bun child process.
  const ipc = typeof process !== "undefined" && typeof process.send === "function"

  export function listen(rpc: Definition) {
    const send = ipc ? (data: string) => process.send!(data) : (data: string) => postMessage(data)

    const handle = async (data: string) => {
      const parsed = JSON.parse(data)
      if (parsed.type === "rpc.request") {
        const result = await rpc[parsed.method](parsed.input)
        send(JSON.stringify({ type: "rpc.result", result, id: parsed.id }))
      }
    }

    if (ipc) {
      process.on("message", (msg: unknown) => handle(msg as string))
    } else {
      onmessage = async (evt) => handle(evt.data)
    }
  }

  export function emit(event: string, data: unknown) {
    const msg = JSON.stringify({ type: "rpc.event", event, data })
    if (ipc) {
      process.send!(msg)
    } else {
      postMessage(msg)
    }
  }

  /** Generic send/receive target — works for both Worker and subprocess. */
  export type Target = {
    send(data: string): void
    receive(handler: (data: string) => void): void
  }
  // kilocode_change end

  export function client<T extends Definition>(target: Target) {
    const pending = new Map<number, { resolve: (result: any) => void; reject: (error: any) => void }>()
    const listeners = new Map<string, Set<(data: any) => void>>()
    let id = 0
    target.receive((data) => {
      const parsed = JSON.parse(data)
      if (parsed.type === "rpc.result") {
        const entry = pending.get(parsed.id)
        if (entry) {
          entry.resolve(parsed.result)
          pending.delete(parsed.id)
        }
      }
      if (parsed.type === "rpc.event") {
        const handlers = listeners.get(parsed.event)
        if (handlers) {
          for (const handler of handlers) {
            handler(parsed.data)
          }
        }
      }
    })
    return {
      call<Method extends keyof T>(method: Method, input: Parameters<T[Method]>[0]): Promise<ReturnType<T[Method]>> {
        const requestId = id++
        return new Promise((resolve, reject) => {
          pending.set(requestId, { resolve, reject })
          target.send(JSON.stringify({ type: "rpc.request", method, input, id: requestId }))
        })
      },
      on<Data>(event: string, handler: (data: Data) => void) {
        let handlers = listeners.get(event)
        if (!handlers) {
          handlers = new Set()
          listeners.set(event, handlers)
        }
        handlers.add(handler)
        return () => {
          handlers!.delete(handler)
        }
      },
      rejectAll(reason: Error) {
        for (const entry of pending.values()) {
          entry.reject(reason)
        }
        pending.clear()
      },
    }
  }
}
