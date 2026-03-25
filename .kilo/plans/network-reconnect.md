# Plan: Graceful Network Disconnect/Reconnect Handling

## Branch: `feat/network-reconnect`

## Prior Art

No existing PRs address this feature. Searched open PRs for "network", "offline", "reconnect", and "connection" — none match. The closest is PR #6802 (SSE reconnect for permission prompts in the VS Code extension), which is a different concern (localhost SSE, not internet connectivity).

## Current State

The codebase has **zero network connectivity awareness**:

- **Only `ECONNRESET` is handled as retryable** (`message-v2.ts:856`). Other network errors (`ETIMEDOUT`, `ENETUNREACH`, `ENOTFOUND`, `ECONNREFUSED`) fall through to `NamedError.Unknown` — **non-retryable** — causing the session loop to stop permanently with an error.
- **No connectivity monitoring** — no probes, no heartbeats, no online/offline state.
- **No "paused" session status** — `SessionStatus` only has `idle`, `busy`, and `retry`.
- **MCP remote servers stay failed** — no automatic reconnection when network returns.
- **The retry loop is blind** — exponential backoff with no awareness of whether the network is actually available.

## Design

### Architecture Overview

```
+-----------------------------------------------------+
|                    Network Monitor                    |
|  (reactive detection + active probing while offline) |
|                                                       |
|  Bus Events: Network.Event.Online / .Offline          |
|  API: waitForOnline(abort): Promise<void>             |
+---------------+----------------------+---------------+
                |                      |
    +-----------v-----------+  +-------v------------------+
    |   Processor (retry)   |  |   MCP Reconnection       |
    |                       |  |   (remote servers)        |
    |  network error ->     |  |                           |
    |  status "network" ->  |  |  online -> reconnect      |
    |  waitForOnline() ->   |  |  failed remote servers    |
    |  NetworkResume.ask()  |  |                           |
    +-----------+-----------+  +---------------------------+
                |
    +-----------v-----------+
    |  NetworkResume         |
    |  (confirmation)        |
    |                        |
    |  Backend promise ->    |
    |  Bus event (SSE) ->    |
    |  TUI/client prompt ->  |
    |  API endpoint reply    |
    +-----------+-----------+
                |
    +-----------v-----------+
    |  TUI Integration       |
    |                        |
    |  Bell + Toast on       |
    |  reconnect. Inline     |
    |  confirmation prompt   |
    +------------------------+
```

### Network Detection Strategy

**Hybrid reactive + active approach:**

1. **Reactive detection**: When an LLM call fails with a network system error code (`ECONNRESET`, `ETIMEDOUT`, `ENETUNREACH`, `ENOTFOUND`, `ECONNREFUSED`), the network monitor is immediately flagged as offline.
2. **Active probing while offline**: Once flagged offline, periodic DNS lookups (`Bun.dns.resolve("dns.google")`) every 2 seconds detect when connectivity returns.
3. **No proactive polling when online**: Zero overhead when things are working — the monitor is dormant until triggered by an error.

### User Confirmation Flow

When the network comes back online while a session was interrupted:

1. **Backend** (`processor.ts`): Detects network error -> sets session status to `{ type: "network" }` -> calls `Network.waitForOnline(abort)` (blocks until DNS probe succeeds) -> calls `NetworkResume.ask(sessionID, abort)` (blocks until user responds)
2. **Backend** (`NetworkResume`): Publishes `NetworkResume.Event.Asked` bus event -> SSE delivers to all clients
3. **TUI**: Receives event -> rings bell -> shows confirmation prompt "Network back online. Do you want to continue?" with [Continue] / [Cancel] options
4. **User responds**: TUI calls `POST /network-resume/:requestID` with `{ action: "continue" | "cancel" }`
5. **Backend**: Promise resolves -> processor either continues retry or breaks the loop

### Mode-Specific Behavior

| Mode                       | Behavior                                                                                                                                                               |
| -------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **TUI** (`kilo`)           | Full prompt: bell + inline confirmation at bottom of session view                                                                                                      |
| **`kilo run`**             | Auto-resume (no user to ask; non-interactive mode)                                                                                                                     |
| **`kilo serve`** (VS Code) | Bus event published over SSE; client handles UI. Initial implementation: auto-resume with toast notification. VS Code extension can add its own confirmation UI later. |

### Session Status Extension

Add a new `"network"` status variant to `SessionStatus.Info`:

```ts
z.object({
  type: z.literal("network"),
  message: z.string(), // e.g., "Network disconnected" or "Waiting for confirmation"
})
```

This allows the TUI to show a distinct status indicator (e.g., "Network offline -- waiting for reconnection...") instead of the generic retry countdown.

---

## Commits

### Commit 1: Add network monitor service

**New file:** `packages/opencode/src/kilocode/network.ts`

```ts
export namespace Network {
  // Bus events
  Event.Online   // published when connectivity is restored
  Event.Offline  // published when connectivity is lost

  // State
  isOnline(): boolean

  // Called reactively when a network error is detected
  markOffline(): void

  // Blocks until network is back online. Uses periodic DNS probes.
  // Respects abort signal for cancellation.
  waitForOnline(abort: AbortSignal): Promise<void>
}
```

Implementation details:

- Uses `Instance.state()` for per-project singleton
- DNS probe: `Bun.dns.resolve("dns.google")` -- lightweight, no HTTP overhead
- Probe interval while offline: 2 seconds
- On successful probe: publish `Network.Event.Online`, stop probing
- On `markOffline()`: publish `Network.Event.Offline`, start probing
- Thread-safe: multiple callers to `waitForOnline()` share the same probe loop

### Commit 2: Handle all network system errors as retryable

**Modified file:** `packages/opencode/src/session/message-v2.ts`

Extend the `fromError()` function (currently line 856) to recognize all network system error codes:

```ts
// Current: only ECONNRESET
case (e as SystemError)?.code === "ECONNRESET":

// New: all network-related system errors
case isNetworkSystemError(e):
  return new MessageV2.APIError({
    message: networkErrorMessage(e),
    isRetryable: true,
    metadata: {
      code: (e as SystemError).code ?? "",
      syscall: (e as SystemError).syscall ?? "",
      message: (e as SystemError).message ?? "",
      isNetwork: true,  // flag for the processor to detect
    },
  }, { cause: e }).toObject()
```

Where `isNetworkSystemError()` checks for: `ECONNRESET`, `ETIMEDOUT`, `ENETUNREACH`, `ENOTFOUND`, `ECONNREFUSED`, `EHOSTUNREACH`, `ENETDOWN`, `EPIPE`, `EAI_AGAIN`.

The `isNetwork: true` metadata flag allows the processor to distinguish network errors from other retryable API errors (like rate limits).

### Commit 3: Add "network" session status + network-aware retry

**Modified files:**

- `packages/opencode/src/session/status.ts` -- Add `"network"` variant to `SessionStatus.Info`
- `packages/opencode/src/session/processor.ts` -- Network-aware retry logic
- `packages/opencode/src/session/retry.ts` -- Add `isNetworkError()` helper

In `processor.ts`, the catch block (currently line 376) gains a new branch:

```ts
} catch (e: any) {
  const error = MessageV2.fromError(e, { providerID: input.model.providerID })

  // ... existing context overflow handling ...

  // NEW: network error handling
  if (isNetworkError(error)) {
    Network.markOffline()
    SessionStatus.set(input.sessionID, {
      type: "network",
      message: "Network disconnected"
    })
    await Network.waitForOnline(input.abort)
    const confirmed = await NetworkResume.ask(input.sessionID, input.abort)
    if (confirmed) {
      attempt = 0  // reset retry counter
      continue
    }
    // User declined -- stop the session
    input.assistantMessage.error = error
    Bus.publish(Session.Event.Error, { ... })
    SessionStatus.set(input.sessionID, { type: "idle" })
    break
  }

  // ... existing retry logic ...
}
```

The `isNetworkError()` helper in `retry.ts` checks for `MessageV2.APIError` with `metadata.isNetwork === true`.

### Commit 4: Network resume confirmation mechanism

**New file:** `packages/opencode/src/kilocode/network-resume.ts`

Follows the same pattern as `Question` (`packages/opencode/src/question/index.ts`):

```ts
export namespace NetworkResume {
  // Bus events
  Event.Asked    // { id, sessionID, message }
  Event.Replied  // { id, sessionID, action: "continue" | "cancel" }

  // Creates a pending confirmation and returns a promise
  // that resolves to true (continue) or false (cancel).
  // In non-interactive contexts (kilo run), auto-resolves to true.
  ask(sessionID: string, abort: AbortSignal): Promise<boolean>

  // Called by the server when the user responds
  reply(id: string, action: "continue" | "cancel"): void

  // List pending confirmations (for SSE reconnect recovery)
  list(): PendingRequest[]
}
```

**New route file:** `packages/opencode/src/server/routes/network-resume.ts`

Add endpoints:

- `GET /network-resume` -- list pending network resume requests
- `POST /network-resume/:requestID` -- reply to a network resume request

**Modified file:** `packages/opencode/src/server/server.ts` -- Register the new route

**Auto-resume for non-interactive modes:**

- Check if the session was created with `permission` rules that deny `question` (this is how `kilo run` configures sessions -- see `run.ts:381-396`)
- If so, auto-resolve to `true` after a short delay (e.g., 1 second)
- This avoids blocking headless/pipeline usage

### Commit 5: TUI integration -- notification and confirmation UI

**Modified files:**

1. **`packages/opencode/src/cli/cmd/tui/context/sync.tsx`**
   - Subscribe to `NetworkResume.Event.Asked` and `NetworkResume.Event.Replied` SSE events
   - Store pending network resume requests in the sync store (similar to `store.question`)
   - Subscribe to `Network.Event.Online` / `Network.Event.Offline` for toast notifications

2. **`packages/opencode/src/cli/cmd/tui/routes/session/index.tsx`**
   - Add network resume prompt rendering in the prompt area (priority: permission > question > network-resume > chat input)
   - Ring bell when `NetworkResume.Event.Asked` fires
   - Show toast "Network disconnected" on offline event
   - Show toast "Network restored" on online event (in addition to the confirmation prompt)

3. **New file: `packages/opencode/src/cli/cmd/tui/routes/session/network-resume.tsx`**
   - Inline prompt component (similar to `QuestionPrompt`):
     - Message: "Network back online. Do you want to continue?"
     - Options: [Continue] [Cancel]
     - Keyboard: Enter to confirm, Escape to cancel, left/right to toggle
   - Calls `sdk.client` on user response

4. **`packages/opencode/src/cli/cmd/tui/routes/session/index.tsx`** -- Status area
   - When session status is `{ type: "network" }`, show "Network offline -- waiting..." in the status area

### Commit 6: MCP server reconnection on network restore

**Modified file:** `packages/opencode/src/mcp/index.ts`

Subscribe to `Network.Event.Online` in the MCP module's `Instance.state()` initialization:

- When online event fires, iterate all MCP servers
- For servers with `status === "failed"` that use remote transport (StreamableHTTP or SSE):
  - Attempt reconnection with the original config
  - Log success/failure
  - Publish toast notification for each reconnected server

This is non-blocking and runs in the background after the network comes back.

### Commit 7: SDK regeneration + kilo run handling

**Run:** `./script/generate.ts` to regenerate `packages/sdk/js/`

The SSE endpoint already uses `Bus.subscribeAll()` (wildcard) so new bus events (`Network.Event.*`, `NetworkResume.Event.*`) will automatically be forwarded to SSE clients. However, we need to:

- Add the new event types to the SDK's event type union so clients get proper typing
- Update `kilo run` in `packages/opencode/src/cli/cmd/run.ts` to handle `NetworkResume.Event.Asked` events (auto-approve when in `--auto` mode or reject the question permission)

### Commit 8: Tests

**New files:**

- `packages/opencode/test/kilocode/network.test.ts` -- Network monitor unit tests
  - Test DNS probe success/failure detection
  - Test `waitForOnline()` resolves when probe succeeds
  - Test abort signal cancels the wait
  - Test bus events are published on transitions

- `packages/opencode/test/kilocode/network-resume.test.ts` -- NetworkResume confirmation tests
  - Test `ask()` creates pending request and resolves on `reply()`
  - Test auto-resume for non-interactive sessions
  - Test abort signal rejects the promise
  - Test `list()` returns pending requests

- Update existing retry tests if the new network error branch affects them

---

## Open Questions / Tradeoffs

### 1. DNS probe target

Using `dns.google` (Google's public DNS) as the probe target. Alternatives: `1.1.1.1` (Cloudflare), `one.one.one.one`. Should we make this configurable, or is a hardcoded well-known target sufficient?

**Recommendation:** Hardcode `dns.google` -- it's the most reliable public DNS. Can make configurable later if needed.

### 2. Probe interval

2-second probe interval while offline. Too aggressive? Too slow?

**Recommendation:** 2 seconds is a good balance. DNS lookup is ~1-2ms when it succeeds. When offline, it fails fast (typically <100ms). Low overhead either way.

### 3. VS Code extension handling

The bus events will be available over SSE for the VS Code extension. Should we implement extension-side handling in this PR or leave it for a follow-up?

**Recommendation:** Leave for follow-up. The extension communicates with a local `kilo serve` process, so the server-side handling (auto-resume or waiting for extension response) is sufficient initially.

### 4. Tool execution failures

When a tool like `websearch` or `webfetch` fails due to network issues, the LLM sees the error and decides what to do next. Should we intercept tool-level network failures too?

**Recommendation:** Not in this PR. Tool failures are already surfaced to the LLM, which can retry or use alternatives. The network monitor's primary job is handling the LLM call -- the most critical network-dependent operation.

### 5. Session persistence across restarts

If the CLI is killed while in "network" status, should it remember and prompt on next launch?

**Recommendation:** No. The session loop state is in-memory. If the process dies, the user restarts and re-sends their message. Consistent with how other interruptions work.
