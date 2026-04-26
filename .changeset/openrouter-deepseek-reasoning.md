---
"@kilocode/cli": patch
---

Fix multi-turn DeepSeek reasoning round-tripping on OpenRouter by bumping `@openrouter/ai-sdk-provider` to 2.8.1 and letting the SDK handle reasoning details, plus pulling in upstream DeepSeek variant, reasoning-effort, and assistant-reasoning fixes.
