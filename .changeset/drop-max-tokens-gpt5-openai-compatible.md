---
"@kilocode/cli": patch
---

Fix gpt-5 models routed through OpenAI-compatible providers (e.g. LiteLLM) rejecting requests with "Unsupported parameter: max_tokens". The CLI now drops the max output token cap for gpt-5 models on `@ai-sdk/openai-compatible`, letting the upstream default output budget apply.
