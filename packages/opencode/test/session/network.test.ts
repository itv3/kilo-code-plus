import { describe, expect, test } from "bun:test"
import { Bus } from "../../src/bus"
import { Instance } from "../../src/project/instance"
import { tmpdir } from "../fixture/fixture"
import { SessionNetwork } from "../../src/session/network"

describe("session.network", () => {
  test("detects common network disconnect codes", () => {
    expect(SessionNetwork.disconnected({ code: "ECONNREFUSED" })).toBe(true)
    expect(SessionNetwork.disconnected({ code: "ENOTFOUND" })).toBe(true)
    expect(SessionNetwork.disconnected({ code: "EAI_AGAIN" })).toBe(true)
    expect(SessionNetwork.disconnected({ code: "ENOENT" })).toBe(false)
  })

  test("reply resolves pending request", async () => {
    await using tmp = await tmpdir({ git: true })
    await Instance.provide({
      directory: tmp.path,
      fn: async () => {
        const ask = SessionNetwork.ask({
          sessionID: "ses_test",
          message: "Connection refused",
          abort: new AbortController().signal,
        })
        const pending = await SessionNetwork.list()
        expect(pending).toHaveLength(1)
        const req = pending[0]!
        await SessionNetwork.reply({ requestID: req.id })
        await expect(ask).resolves.toBeUndefined()
      },
    })
  })

  test("reject rejects pending request", async () => {
    await using tmp = await tmpdir({ git: true })
    await Instance.provide({
      directory: tmp.path,
      fn: async () => {
        const ask = SessionNetwork.ask({
          sessionID: "ses_test",
          message: "Connection timed out",
          abort: new AbortController().signal,
        })
        const pending = await SessionNetwork.list()
        expect(pending).toHaveLength(1)
        const req = pending[0]!
        await SessionNetwork.reject({ requestID: req.id })
        await expect(ask).rejects.toBeInstanceOf(SessionNetwork.RejectedError)
      },
    })
  })

  test("aborted signal rejects without publishing asked", async () => {
    await using tmp = await tmpdir({ git: true })
    await Instance.provide({
      directory: tmp.path,
      fn: async () => {
        const abort = new AbortController()
        const seen: string[] = []
        const offAsked = Bus.subscribe(SessionNetwork.Event.Asked, () => seen.push("asked"))
        const offRejected = Bus.subscribe(SessionNetwork.Event.Rejected, () => seen.push("rejected"))
        abort.abort()

        try {
          const err = await SessionNetwork.ask({
            sessionID: "ses_test",
            message: "Connection timed out",
            abort: abort.signal,
          }).catch((err) => err)

          expect(err).toBeInstanceOf(DOMException)
          expect(err.name).toBe("AbortError")
          expect(await SessionNetwork.list()).toHaveLength(0)
          expect(seen).toStrictEqual(["rejected"])
        } finally {
          offAsked()
          offRejected()
        }
      },
    })
  })
})
