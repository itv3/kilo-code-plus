// kilocode_change - new file
// Integration tests for the caps + worker offload applied inside Snapshot.diffFull.
// Asserts the freeze repro workload finishes quickly and that the event loop
// stays responsive while the diff runs.

import { test, expect, afterAll } from "bun:test"
import { $ } from "bun"
import { Snapshot } from "../../src/snapshot"
import { Instance } from "../../src/project/instance"
import { Filesystem } from "../../src/util/filesystem"
import { Log } from "../../src/util/log"
import { DiffEngine } from "../../src/kilocode/snapshot/diff-engine"
import { tmpdir } from "../fixture/fixture"

Log.init({ print: false })

afterAll(async () => {
  await DiffEngine.shutdown()
})

async function bootstrap(setup: (dir: string) => Promise<void>) {
  return tmpdir({
    git: true,
    init: async (dir) => {
      await setup(dir)
      await $`git add .`.cwd(dir).quiet()
      await $`git commit --no-gpg-sign -m init`.cwd(dir).quiet()
    },
  })
}

test("diffFull returns empty patch with counted adds/deletes when a file crosses the line cap", async () => {
  // 5000-line file (cap is 2000) — the underlying Myers would take minutes.
  const big = Array.from({ length: 5000 }, (_, i) => `v1_${i}`).join("\n") + "\n"
  const bigAfter = Array.from({ length: 5000 }, (_, i) => `v2_${i}`).join("\n") + "\n"

  await using tmp = await bootstrap(async (dir) => {
    await Filesystem.write(`${dir}/big.txt`, big)
  })

  await Instance.provide({
    directory: tmp.path,
    fn: async () => {
      const before = await Snapshot.track()
      expect(before).toBeTruthy()

      await Filesystem.write(`${tmp.path}/big.txt`, bigAfter)
      const after = await Snapshot.track()
      expect(after).toBeTruthy()

      const start = Date.now()
      const diffs = await Snapshot.diffFull(before!, after!)
      const elapsed = Date.now() - start

      expect(diffs).toHaveLength(1)
      const [hit] = diffs
      expect(hit).toBeDefined()
      expect(hit!.file).toBe("big.txt")
      // Skipped = empty patch, but add/delete counts come from git numstat.
      expect(hit!.patch).toBe("")
      expect(hit!.additions).toBeGreaterThan(0)
      expect(hit!.deletions).toBeGreaterThan(0)
      // Must finish in well under a second — without the caps this ran for minutes.
      expect(elapsed).toBeLessThan(3000)
    },
  })
})

test("diffFull keeps full patches for small files in a workload", async () => {
  await using tmp = await bootstrap(async (dir) => {
    for (let i = 0; i < 20; i++) {
      await Filesystem.write(`${dir}/f_${i}.txt`, `hello ${i}\nline 2\n`)
    }
  })

  await Instance.provide({
    directory: tmp.path,
    fn: async () => {
      const before = await Snapshot.track()
      expect(before).toBeTruthy()

      for (let i = 0; i < 20; i++) {
        await Filesystem.write(`${tmp.path}/f_${i}.txt`, `world ${i}\nline 2\n`)
      }
      const after = await Snapshot.track()
      expect(after).toBeTruthy()

      const start = Date.now()
      const diffs = await Snapshot.diffFull(before!, after!)
      const elapsed = Date.now() - start

      expect(diffs.length).toBe(20)
      for (const d of diffs) {
        expect(d.patch).toContain("@@")
        expect(d.patch.length).toBeGreaterThan(0)
      }
      expect(elapsed).toBeLessThan(5000)
    },
  })
})

test("diffFull mixes skipped and non-skipped files in one result", async () => {
  const huge = Array.from({ length: 5000 }, (_, i) => `h_${i}`).join("\n") + "\n"
  const hugeAfter = Array.from({ length: 5000 }, (_, i) => `k_${i}`).join("\n") + "\n"

  await using tmp = await bootstrap(async (dir) => {
    await Filesystem.write(`${dir}/tiny.txt`, "one\ntwo\n")
    await Filesystem.write(`${dir}/huge.txt`, huge)
  })

  await Instance.provide({
    directory: tmp.path,
    fn: async () => {
      const before = await Snapshot.track()
      expect(before).toBeTruthy()

      await Filesystem.write(`${tmp.path}/tiny.txt`, "one\nchanged\n")
      await Filesystem.write(`${tmp.path}/huge.txt`, hugeAfter)
      const after = await Snapshot.track()
      expect(after).toBeTruthy()

      const diffs = await Snapshot.diffFull(before!, after!)
      const tiny = diffs.find((d) => d.file === "tiny.txt")
      const big = diffs.find((d) => d.file === "huge.txt")
      expect(tiny).toBeDefined()
      expect(big).toBeDefined()
      expect(tiny!.patch).toContain("@@")
      expect(big!.patch).toBe("")
      // numstat still populates additions/deletions for the skipped file.
      expect(big!.additions).toBeGreaterThan(0)
      expect(big!.deletions).toBeGreaterThan(0)
    },
  })
})

test("event loop stays responsive while diffFull runs a heavy workload", async () => {
  // 40 files of ~200 lines each. Under the old code each file went through a
  // synchronous structuredPatch on the worker thread. Under the new code the
  // yields + worker offload keep the event loop breathing.
  await using tmp = await bootstrap(async (dir) => {
    for (let i = 0; i < 40; i++) {
      const text = Array.from({ length: 200 }, (_, j) => `f_${i}_line_${j}`).join("\n") + "\n"
      await Filesystem.write(`${dir}/mod_${i}.txt`, text)
    }
  })

  await Instance.provide({
    directory: tmp.path,
    fn: async () => {
      const before = await Snapshot.track()
      expect(before).toBeTruthy()

      for (let i = 0; i < 40; i++) {
        const text = Array.from({ length: 200 }, (_, j) => `f_${i}_line_${j}_v2`).join("\n") + "\n"
        await Filesystem.write(`${tmp.path}/mod_${i}.txt`, text)
      }
      const after = await Snapshot.track()
      expect(after).toBeTruthy()

      // Count how many times a concurrent interval fires during the diff.
      // If the event loop stays responsive, we expect several ticks.
      let ticks = 0
      const timer = setInterval(() => {
        ticks++
      }, 25)
      try {
        await Snapshot.diffFull(before!, after!)
      } finally {
        clearInterval(timer)
      }
      // Even on a slow CI box the interval should fire multiple times if we are
      // not blocking the loop. This is the honest "ESC still works" signal.
      expect(ticks).toBeGreaterThan(0)
    },
  })
})
