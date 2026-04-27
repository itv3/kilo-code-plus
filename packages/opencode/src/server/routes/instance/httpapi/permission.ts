import { Permission } from "@/permission"
import { PermissionID } from "@/permission/schema"
import { Effect, Layer, Schema } from "effect"
import { HttpApi, HttpApiBuilder, HttpApiEndpoint, HttpApiError, HttpApiGroup, OpenApi } from "effect/unstable/httpapi"

const root = "/permission"
const SaveAlwaysRulesBody = Schema.Struct({
  approvedAlways: Schema.Array(Schema.String).pipe(Schema.optional),
  deniedAlways: Schema.Array(Schema.String).pipe(Schema.optional),
})

export const PermissionApi = HttpApi.make("permission")
  .add(
    HttpApiGroup.make("permission")
      .add(
        HttpApiEndpoint.get("list", root, {
          success: Schema.Array(Permission.Request),
        }).annotateMerge(
          OpenApi.annotations({
            identifier: "permission.list",
            summary: "List pending permissions",
            description: "Get all pending permission requests across all sessions.",
          }),
        ),
        HttpApiEndpoint.post("reply", `${root}/:requestID/reply`, {
          params: { requestID: PermissionID },
          payload: Permission.ReplyBody,
          success: Schema.Boolean,
          // kilocode_change - surface stale permission replies as 404 in experimental HttpApi mode too
          error: [HttpApiError.NotFoundNoContent],
        }).annotateMerge(
          OpenApi.annotations({
            identifier: "permission.reply",
            summary: "Respond to permission request",
            description: "Approve or deny a permission request from the AI assistant.",
          }),
        ),
        HttpApiEndpoint.post("saveAlwaysRules", `${root}/:requestID/always-rules`, {
          params: { requestID: PermissionID },
          payload: SaveAlwaysRulesBody,
          success: Schema.Boolean,
          // kilocode_change - keep experimental HttpApi parity with classic route semantics
          error: [HttpApiError.NotFoundNoContent],
        }).annotateMerge(
          OpenApi.annotations({
            identifier: "permission.saveAlwaysRules",
            summary: "Save always-allow/deny permission rules",
            description: "Save approved/denied always-rules for a pending permission request.",
          }),
        ),
      )
      .annotateMerge(
        OpenApi.annotations({
          title: "permission",
          description: "Experimental HttpApi permission routes.",
        }),
      ),
  )
  .annotateMerge(
    OpenApi.annotations({
      title: "opencode experimental HttpApi",
      version: "0.0.1",
      description: "Experimental HttpApi surface for selected instance routes.",
    }),
  )

export const permissionHandlers = Layer.unwrap(
  Effect.gen(function* () {
    const svc = yield* Permission.Service

    const list = Effect.fn("PermissionHttpApi.list")(function* () {
      return yield* svc.list()
    })

    const reply = Effect.fn("PermissionHttpApi.reply")(function* (ctx: {
      params: { requestID: PermissionID }
      payload: Permission.ReplyBody
    }) {
      const ok = yield* svc.reply({
        requestID: ctx.params.requestID,
        reply: ctx.payload.reply,
        message: ctx.payload.message,
      })
      // kilocode_change - match the classic Hono route so clients can stale-clean up
      if (!ok) return yield* new HttpApiError.NotFound({})
      return true
    })

    const saveAlwaysRules = Effect.fn("PermissionHttpApi.saveAlwaysRules")(function* (ctx: {
      params: { requestID: PermissionID }
      payload: Schema.Schema.Type<typeof SaveAlwaysRulesBody>
    }) {
      const ok = yield* svc.saveAlwaysRules({
        requestID: ctx.params.requestID,
        approvedAlways: ctx.payload.approvedAlways ? [...ctx.payload.approvedAlways] : undefined,
        deniedAlways: ctx.payload.deniedAlways ? [...ctx.payload.deniedAlways] : undefined,
      })
      // kilocode_change - match the classic Hono route so clients can stale-clean up
      if (!ok) return yield* new HttpApiError.NotFound({})
      return true
    })

    return HttpApiBuilder.group(PermissionApi, "permission", (handlers) =>
      handlers.handle("list", list).handle("reply", reply).handle("saveAlwaysRules", saveAlwaysRules),
    )
  }),
).pipe(Layer.provide(Permission.defaultLayer))
