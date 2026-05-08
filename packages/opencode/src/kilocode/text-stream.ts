import { createReadStream } from "fs"
import { PassThrough, Readable } from "stream"
import * as Encoding from "./encoding"

/**
 * Encoding-aware text streaming for tools that walk a file line by line.
 *
 * Most files we read are UTF-8, so we stream chunks straight from disk
 * (decoded strictly with `TextDecoder({ fatal: true })`) and let the consumer
 * early-exit without buffering the rest of the file. Only when the bytes turn
 * out not to be valid UTF-8 do we fall back to {@link Encoding.read}, which
 * runs full-file detection through iconv-lite.
 *
 * Consumers should import this module as a namespace:
 *   import * as TextStream from "../kilocode/text-stream"
 */

/**
 * Sentinel error used to signal the optimistic UTF-8 stream gave up because
 * the bytes are not valid UTF-8. Distinct class so {@link withFallback} can
 * tell it apart from real I/O failures.
 */
export class InvalidUtf8Error extends Error {
  constructor() {
    super("invalid utf-8")
  }
}

/**
 * UTF-8 text Readable for `filepath`. Streams chunks straight from disk so the
 * caller can early-exit without buffering the rest of the file. If invalid
 * UTF-8 is encountered the stream is destroyed with an {@link
 * InvalidUtf8Error}. A leading UTF-8 BOM passes through as U+FEFF — the same
 * behaviour as `createReadStream({ encoding: "utf8" })`.
 */
export function openUtf8(filepath: string): Readable {
  const out = new PassThrough({ encoding: "utf8" })
  const raw = createReadStream(filepath)
  const decoder = new TextDecoder("utf-8", { fatal: true })
  raw.on("data", (chunk) => {
    try {
      const text = decoder.decode(chunk as Buffer, { stream: true })
      if (text) out.write(text)
    } catch {
      raw.destroy()
      out.destroy(new InvalidUtf8Error())
    }
  })
  raw.on("end", () => {
    try {
      const tail = decoder.decode()
      if (tail) out.write(tail)
      out.end()
    } catch {
      out.destroy(new InvalidUtf8Error())
    }
  })
  raw.on("error", (err) => out.destroy(err))
  return out
}

/**
 * Whole-file UTF-8 text Readable for `filepath`, decoded via
 * {@link Encoding.read}'s detection logic. Buffers the entire decoded file
 * into memory; used as a fallback for files that {@link openUtf8} rejects.
 */
export async function openDecoded(filepath: string): Promise<Readable> {
  const decoded = await Encoding.read(filepath)
  return Readable.from([decoded.text])
}

/**
 * Run `fn` against an optimistic UTF-8 stream of `filepath`. If the bytes
 * turn out not to be valid UTF-8, `fn` is run a second time against a
 * fallback iconv-decoded stream. Other errors are propagated unchanged.
 */
export async function withFallback<T>(filepath: string, fn: (input: Readable) => Promise<T>): Promise<T> {
  try {
    return await fn(openUtf8(filepath))
  } catch (err) {
    if (!(err instanceof InvalidUtf8Error)) throw err
  }
  return fn(await openDecoded(filepath))
}
