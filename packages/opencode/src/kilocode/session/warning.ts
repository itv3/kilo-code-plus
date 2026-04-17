// kilocode_change - new file
//
// Dedicated bus event for "something went wrong but we recovered" signals,
// such as skipped diffs in summary computation. Lives in its own module so
// both `session/` (summary emitter) and `snapshot/` (diff emitter) can import
// it without creating a cycle.

import z from "zod"
import { BusEvent } from "../../bus/bus-event"
import { SessionID } from "../../session/schema"

export const SessionWarningEvent = BusEvent.define(
  "session.warning",
  z.object({
    sessionID: SessionID.zod.optional(),
    kind: z.enum(["diff_skipped", "summary_truncated", "summary_failed"]),
    message: z.string(),
    details: z.record(z.string(), z.unknown()).optional(),
  }),
)
