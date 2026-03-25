# Token & Request Budget Limits

**Priority:** P2

The legacy extension had controls to limit agent resource consumption per task. The new extension has no budget limit controls.

## Legacy Features

### Max Requests per Task

- `allowedMaxRequests` — maximum number of API requests before pausing
- When the limit was reached, the agent paused and asked the user for approval to continue
- Prevented runaway agent loops that could make hundreds of requests

### Max Cost per Task

- `allowedMaxCost` — maximum total API cost (in dollars) before pausing
- Provided budgeting control for expensive operations
- Users could set a per-task spending cap

### Auto-Condense Threshold

- `autoCondenseContextPercent` — percentage of context window at which condensation triggers (default: 100%)
- Configurable to trigger earlier (e.g., 80%) for more aggressive context management

### Separate Condensation Model

- `condensingApiConfigId` — use a different (cheaper) model for context condensation
- Reduced costs by using a smaller model for summarization work

## Current State in New Extension

- The CLI has per-agent `steps` limit (max steps) — already exposed in settings
- Auto compaction toggle exists but no threshold configuration
- No per-session cost or request limits
- No separate model selection for compaction

## Remaining Work

- **Evaluate CLI support**: Determine if the CLI supports or can support:
  - Per-session request count limits (beyond per-agent `steps`)
  - Per-session cost tracking and limits
  - Compaction threshold configuration
  - Separate model for compaction
- **Steps limit documentation**: The per-agent `steps` field is already exposed — ensure it's clearly documented as the equivalent of `maxRequests`
- **Cost tracking UI**: If the CLI tracks per-session costs, display them in the session header and add an optional limit setting
- **Compaction threshold**: If the CLI's `compaction` config supports a threshold value, expose it in the Context tab

## Notes

The per-agent `steps` field in the CLI may serve as the equivalent of `allowedMaxRequests`. Cost tracking depends on the CLI's ability to calculate and report costs, which varies by provider. If the CLI doesn't support cost limits, this may need to be a CLI-side feature request.
