# Provider-Specific Settings

**Priority:** P2

The legacy extension had extensive per-provider settings that tuned API behaviour for each provider. The new extension has provider connection (API key, base URL) but no provider-specific tuning options.

## Legacy Features

### OpenRouter

- `openRouterSpecificProvider` — force a specific backend provider
- `openRouterProviderDataCollection` — `"allow"` / `"deny"` data collection preference
- `openRouterProviderSort` — sort providers by `"price"` / `"throughput"` / `"latency"`
- `openRouterZdr` — zero-data retention toggle

### Anthropic

- `anthropicBeta1MContext` — enable 1M context beta
- `anthropicDeploymentName` — custom deployment name

### OpenAI

- `openAiNativeServiceTier` — `"default"` / `"flex"` / `"priority"` tier selection
- `openAiR1FormatEnabled` — R1-format reasoning output
- `openAiStreamingEnabled` — enable/disable streaming per provider

### AWS Bedrock

- `awsUseCrossRegionInference` — cross-region inference routing
- `awsUseGlobalInference` — global inference
- `awsUsePromptCache` — prompt caching
- `awsBedrock1MContext` — 1M context beta
- `awsBedrockServiceTier` — `"STANDARD"` / `"FLEX"` / `"PRIORITY"`

### Google Vertex

- `enableUrlContext` — URL context in requests
- `enableGrounding` — grounding feature
- `vertex1MContext` — 1M context beta

### Ollama

- `ollamaNumCtx` — context size override (min 128)

### LM Studio

- `lmStudioDraftModelId` — draft model for speculative decoding
- `lmStudioSpeculativeDecodingEnabled` — enable speculative decoding

### General Per-Provider

- `rateLimitSeconds` — delay between requests (per provider profile)
- `rateLimitAfter` — apply rate limit after request instead of before
- `requestDelaySeconds` — global delay between requests

## Current State in New Extension

- Provider tab has connect/disconnect UI with API key entry
- Custom provider dialog supports base URL and API key
- No provider-specific tuning settings
- The CLI backend may handle some of these through provider config options

## Remaining Work

- **Audit CLI provider options**: Determine which provider-specific settings the CLI's `provider[name].options` supports. The CLI may already handle some of these (e.g., OpenRouter routing preferences) through its provider config
- **Provider settings form**: Add a per-provider settings panel (accessible from the Providers tab) that shows only the settings relevant to that provider type
- **Rate limiting**: If the CLI supports configurable rate limits per provider, expose them in the provider settings
- **Model whitelisting**: The CLI supports `provider[name].whitelist` and `provider[name].blacklist` — add UI to filter which models appear for a provider

## Notes

Many of these settings were necessary because the legacy extension made API calls directly. In the CLI architecture, the CLI handles all API communication and may have its own provider configuration. Some settings may be CLI config options rather than extension settings. Audit the CLI's provider config schema before implementing UI.
