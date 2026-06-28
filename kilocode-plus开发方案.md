# Kilo Code Plus · 开发方案

## 0. 目标与结论

基于 Kilo Code v2 做一个自定义发行版:

1. **自定义提供商UI增强**:支持 OpenAI / Anthropic / Gemini 三种 API 类型;模型自动发现;模型能力与高级参数配置;成本选项;添加模型时按模型 ID 匹配内置默认值。
2. **模型列表过滤**:默认只显示 Kilo 自带免费模型;用户后续添加的 provider 模型继续显示。
3. **智能体中文化**:补齐内置智能体描述中文,从可见列表隐藏冗余的 `orchestrator` 智能体。
4. **自动发布**:跟随上游 merge + tag,CI 自动发布到 Open VSX 和微软 Marketplace。

结论:四项均可行,整体工作量 **中**。需求一、需求二、需求三已完成实现并本地打包安装到 Cursor;后续主要改动集中在扩展标识、发布配置和端到端回归验证。

基础约束:

| 项 | 结论 |
|---|---|
| 产品名 | `Kilo Code Plus` |
| 目标编辑器 | Cursor + VS Code |
| 工程基础 | Kilo Code v2 `main` / `master` |
| 内部命令 ID | 保留 `kilo-code.*` / `kilo-code-ActivityBar`,避免命令和视图失效 |
| 维护方式 | 定期 merge 上游,打 tag 后自动发布 |

最小入侵边界:

1. 自定义 provider 增强尽量只改 `packages/kilo-vscode`,不碰 `packages/opencode/src/provider/provider.ts`;底层已经支持 `@ai-sdk/google`、`modalities`、`limit`。
2. 智能体中文化只做 UI 展示映射,不改 agent 内部 `name`,不翻系统 prompt。
3. `orchestrator` 不做大范围删除;当前实现只在 VS Code 扩展展示/可选列表里隐藏,不改核心 agent 定义。

---

## 1. 需求一 · 自定义提供商UI增强

### 1.1 已实现功能

| 功能 | 说明 |
|---|---|
| API 格式 | 自定义提供商支持 OpenAI / Anthropic / Gemini 三种 原生格式API |
| 模型自动发现 | 支持 OpenAI、Anthropic、Gemini,并自动处理 endpoint 和认证头 |
| 模型能力配置 | 支持添加图像输入能力、推理能力 |
| 高级模型参数 | 支持 `context token limit`、`output token limit` |
| 成本选项 | 支持输入、输出、缓存读取、缓存写入成本 |
| 默认值补全 | 添加模型时用模型 ID 匹配内置默认模型,匹配上后自动带出高级模型参数、图像输入能力、推理能力、成本 |
| 保存/编辑 | 支持正确写入或移除 `limit`、`modalities`、`reasoning`、`cost` |

### 1.2 API 类型与模型发现

`form.npm` 到发现协议的映射:

| `form.npm` | 协议 | 发现方式 |
|---|---|---|
| `@ai-sdk/openai-compatible` | `openai` | `GET {baseURL}/models` + Bearer key |
| `@ai-sdk/openai` | `openai` | `GET {baseURL}/models` + Bearer key |
| `@ai-sdk/anthropic` | `anthropic` | `GET {baseURL}/models` + `x-api-key` / `anthropic-version` |
| `@ai-sdk/google` | `gemini` | `GET {baseURL}/models` + Gemini key |

`baseURL` 规范化:

| API 类型 | 规则 |
|---|---|
| OpenAI / OpenAI Compatible | 保持用户输入,发现时拼 `/models` |
| Anthropic | 自动补到 `/v1`,发现时拼 `/models` |
| Gemini | 自动补到 `/v1beta`,发现时拼 `/models` |

### 1.3 模型能力、高级参数与成本

模型卡片新增字段:

| UI 字段 | 保存字段 |
|---|---|
| 图像 | `models.*.modalities.input` |
| 推理 | `models.*.reasoning` |
| 上下文 token 上限 | `models.*.limit.context` |
| 最大输出 token | `models.*.limit.output` |
| 输入成本 | `models.*.cost.input` |
| 输出成本 | `models.*.cost.output` |
| 缓存读取 | `models.*.cost.cache_read` |
| 缓存写入 | `models.*.cost.cache_write` |

保存规则:

```ts
entry.modalities = {
  input: model.image ? ["text", "image"] : ["text"],
  output: ["text"],
}
```

成本字段只有勾选“成本选项”时才校验和保存。未勾选时,即使输入框里曾经有值,也不会写入 `cost`。

### 1.4 默认值匹配

添加自动发现到的模型时,按模型 ID 匹配内置 provider catalog:

| 当前 API 类型 | 匹配内置 provider |
|---|---|
| OpenAI / OpenAI Compatible | `openai` |
| Anthropic | `anthropic` |
| Gemini | `google` |

匹配规则:

1. 优先使用模型发现接口返回的字段。
2. 发现接口没返回时,用模型 ID 匹配内置默认模型。
3. 匹配成功后自动带出 `limit`、`modalities`、`reasoning`、`cost`。
4. `claude-opus-4-8` 作为特殊兼容项,回退匹配 `claude-opus-4-7`。

### 1.5 涉及文件

| 文件 | 用途 |
|---|---|
| `packages/kilo-vscode/src/KiloProvider.ts` | 分发模型发现请求 |
| `packages/kilo-vscode/src/provider-actions.ts` | 保存自定义 provider 时兼容扩展后的配置类型 |
| `packages/kilo-vscode/src/shared/custom-provider.ts` | schema、normalize、删除哨兵支持 `modalities` / `limit` / `cost` |
| `packages/kilo-vscode/src/shared/fetch-models.ts` | OpenAI / Anthropic / Gemini 模型发现与价格解析 |
| `packages/kilo-vscode/src/shared/provider-model.ts` | 支持 `@ai-sdk/google` 和 baseURL 规范化 |
| `packages/kilo-vscode/webview-ui/src/components/settings/CustomProviderDialog.tsx` | 表单状态、模型发现、默认值匹配、添加模型 |
| `packages/kilo-vscode/webview-ui/src/components/settings/CustomProviderModelCard.tsx` | 模型卡片 UI 字段 |
| `packages/kilo-vscode/webview-ui/src/components/settings/CustomProviderValidation.ts` | 表单校验与保存序列化 |
| `packages/kilo-vscode/webview-ui/src/i18n/en.ts` | 英文文案 |
| `packages/kilo-vscode/webview-ui/src/i18n/zh.ts` | 中文文案 |
| `packages/kilo-vscode/webview-ui/src/types/messages/extension-messages.ts` | extension -> webview 消息类型 |
| `packages/kilo-vscode/webview-ui/src/types/messages/webview-messages.ts` | webview -> extension 消息类型 |
| `packages/kilo-vscode/tests/unit/custom-provider-dialog-validate.test.ts` | 表单校验与保存测试 |
| `packages/kilo-vscode/tests/unit/custom-provider.test.ts` | 自定义 provider schema / 删除哨兵测试 |
| `packages/kilo-vscode/tests/unit/fetch-models.test.ts` | 模型发现解析测试 |

---

## 2. 需求二 · 模型列表过滤

### 2.1 已实现行为

| 用户配置状态 | 模型列表显示 |
|---|---|
| 未添加任何 provider 或自定义模型 | Kilo 自带免费模型 |
| 添加 OpenAI 订阅 | Kilo 自带免费模型 + OpenAI 订阅模型 |
| 再添加中转站 | Kilo 自带免费模型 + OpenAI 订阅模型 + 中转站模型 |

过滤发生在 VS Code 扩展端,不改 CLI provider 发现逻辑。扩展从后端拿到 `response.all` 和 `response.connected` 后,先通过 `filterProviders(response.all, response.connected)` 过滤,再 `indexProvidersById(providers)` 发给 webview。

### 2.2 过滤规则

```ts
const connected = new Set(response.connected)

const providers = response.all
  .map((provider) => {
    const models =
      provider.id === "kilo"
        ? pickBy(provider.models, (model) => model.isFree === true)
        : connected.has(provider.id)
          ? provider.models
          : {}

    return { ...provider, models }
  })
  .filter((provider) => Object.keys(provider.models).length > 0)
```

实际代码使用 `KILO_PROVIDER_ID` 判断 Kilo 官方 provider:

| provider | 规则 |
|---|---|
| `kilo` | 只保留 `model.isFree === true` 的免费模型 |
| 已连接 provider | 保留该 provider 的全部模型 |
| 未连接 provider | 清空模型;清空后如果没有可见模型,整个 provider 不下发给 webview |

注意:不要用 `provider.source === "custom"` 判断用户自定义添加。models.dev 目录导入的普通 provider 也可能带 `source: "custom"`,会误放出未添加模型。

### 2.3 涉及文件

| 文件 | 用途 |
|---|---|
| `packages/kilo-vscode/src/KiloProvider.ts` | 在 `providersLoaded` 前调用 `filterProviders`,过滤后再下发 provider 列表 |
| `packages/kilo-vscode/src/kilo-provider-utils.ts` | 新增 `filterProviders`,封装 Kilo 免费模型和 connected provider 过滤逻辑 |
| `packages/kilo-vscode/tests/unit/kilo-provider-utils.test.ts` | 覆盖 Kilo 免费模型、已连接 provider、未连接 custom provider、空模型 provider |
| `.changeset/filter-visible-models.md` | 需求二发布说明 |

本需求没有修改 `packages/opencode`。`response.connected` 仍由后端现有 provider 接口生成,扩展层只消费该结果。

---

## 3. 需求三 · 智能体中文化与隐藏 orchestrator

### 3.1 已实现行为

中文化范围以“智能体行为 > 代理”页面和 agent picker 的可见列表为准。当前实现只翻译内置智能体的描述,不翻译内部 `name`,也不翻译系统 prompt。

| 项 | 当前处理 |
|---|---|
| `ask` / `code` / `debug` / `explore` / `general` / `plan` 描述 | 通过 i18n key 显示中文描述 |
| 内置智能体显示名 | 保留后端 `displayName`;没有 `displayName` 时按 slug 格式化 |
| 自定义智能体描述 | 保留用户/配置提供的原描述 |
| `orchestrator` | 从 VS Code 扩展可见列表隐藏 |
| 系统 prompt | 不翻译、不修改 |

系统提示词影响模型行为,且上游可能频繁优化;翻译会增加冲突和效果风险。`orchestrator` 也不删除核心定义,只在扩展层过滤,降低后续合并上游的冲突面。

### 3.2 可见列表过滤点

| 位置 | 处理 |
|---|---|
| `KiloProvider.fetchAndSendAgents()` | 后端返回 agents 后先调用 `filterAgents`,再生成 `agentsLoaded` |
| `filterVisibleAgents()` | 继续过滤 `subagent` 和 `hidden`,并额外排除 `orchestrator` |
| `AgentBehaviourTab` | 从配置对象补充 agent 名称时,过滤 `orchestrator` |
| `ModeSwitcher` | agent picker 使用 `agentDescription()` 获取本地化描述 |

### 3.3 涉及文件

| 文件 | 用途 |
|---|---|
| `packages/kilo-vscode/src/KiloProvider.ts` | agents 下发前调用 `filterAgents`,使 webview 收不到 `orchestrator` |
| `packages/kilo-vscode/src/kilo-provider-utils.ts` | 新增 `filterAgents`,并让 `filterVisibleAgents` 排除 `orchestrator` |
| `packages/kilo-vscode/webview-ui/src/utils/agent-display.ts` | 新增内置 agent 描述 key、显示名格式化、隐藏 agent 判断 |
| `packages/kilo-vscode/webview-ui/src/components/shared/ModeSwitcher.tsx` | agent picker 使用 `agentLabel` 和 `agentDescription` |
| `packages/kilo-vscode/webview-ui/src/components/settings/AgentBehaviourTab.tsx` | 设置页使用本地化描述,并过滤配置里的 `orchestrator` |
| `packages/kilo-vscode/webview-ui/src/i18n/en.ts` | 新增 6 个内置 agent 描述英文 key |
| `packages/kilo-vscode/webview-ui/src/i18n/zh.ts` | 新增 6 个内置 agent 中文描述 |
| `packages/kilo-vscode/tests/unit/agent-display.test.ts` | 覆盖描述本地化、自定义描述保留、显示名 fallback、`orchestrator` 隐藏 |
| `packages/kilo-vscode/tests/unit/kilo-provider-utils.test.ts` | 覆盖 `filterAgents` 和 `filterVisibleAgents` 对 `orchestrator` 的过滤 |
| `.changeset/localize-agent-descriptions.md` | 需求三发布说明 |

本需求没有修改 `packages/opencode/src/agent/agent.ts`、`packages/opencode/src/kilocode/agent/index.ts` 或 `packages/kilo-indexing/src/indexing/orchestrator.ts`。

---

## 4. 需求四 · 扩展标识与自动发布

### 4.1 实际扩展标识

| 字段 | 实际值 |
|---|---|
| `displayName` | `Kilo Code Plus` |
| `name` | `kilo-code-plus` |
| `publisher` | `itv3` |
| 市场扩展 ID | `itv3.kilo-code-plus` |
| GitHub 仓库 | `https://github.com/itv3/kilo-code-plus` |
| `repository.url` | `https://github.com/itv3/kilo-code-plus.git` |
| 当前市场版本 | `7.3.5401` |
| 当前发布批次 | `7.3.54-v0.01` |

不要改 `contributes` 里的 `kilo-code.*`、`kilo-code-ActivityBar`、命令 ID 和视图 ID。它们属于运行时和 UI 绑定标识,改动会扩大入侵面并容易导致已有命令、视图、状态迁移失效。

改名后已修正扩展运行时版本读取逻辑:不再只查官方扩展 ID `kilocode.kilo-code`,而是优先读当前 `ExtensionContext.extension.packageJSON.version`,避免“关于 Kilo Code”页面显示 `unknown`。

### 4.2 版本规则

VS Marketplace / Open VSX 的 `package.json.version` 必须是普通 SemVer 版本,不能使用 `7.3.54-v0.01` 这种带自定义后缀的市场版本。因此 Plus 版本拆成两层:

| 项 | 规则 | 示例 |
|---|---|---|
| 市场版本 | 写入 `packages/kilo-vscode/package.json.version` | `7.3.5401` |
| 发布批次 | 描述基于哪个上游版本和 Plus 自定义版本 | `7.3.54-v0.01` |
| tag | `kilo-code-plus-v<发布批次>` | `kilo-code-plus-v7.3.54-v0.01` |
| GitHub Release | 与 tag 一致 | `kilo-code-plus-v7.3.54-v0.01` |
| VSIX 名称 | `kilo-code-plus-<发布批次>-<target>.vsix` | `kilo-code-plus-7.3.54-v0.01-darwin-arm64.vsix` |

文档表述统一为:基于上游 `7.3.54`,Plus 自定义版本 `v0.01`。

### 4.3 自动发布

| 任务 | 文件/配置 |
|---|---|
| 自动发布 workflow | `.github/workflows/publish-kilo-code-plus.yml` |
| 触发方式 | 推送 `kilo-code-plus-v*` tag,或手动 `workflow_dispatch` |
| 发布目标 | VS Marketplace、Open VSX、GitHub Release |
| 当前 target | `darwin-arm64` |
| VS Marketplace token | GitHub Secrets: `VSCE_TOKEN` |
| Open VSX token | GitHub Secrets: `OVSX_TOKEN`;兼容保留 `OPENVSX_TOKEN` |
| GitHub Release token | workflow 使用 `github.token` |

自动发布流程:

1. 解析 tag 或手动输入的发布批次。
2. 读取 `packages/kilo-vscode/package.json.version` 作为市场版本。
3. 构建扩展和 CLI binary。
4. 打包 `darwin-arm64` VSIX。
5. 发布到 VS Marketplace。
6. 发布到 Open VSX。
7. 创建或更新 GitHub Release,并上传 VSIX。

### 4.4 本地编译与打包

标准本地构建命令:

```sh
cd /Users/czs/Developer/kilocode-plus/packages/kilo-vscode
bun run prepare:cli-binary -- --force
bun run rebuild-sdk
bun run typecheck
node esbuild.js --production
./node_modules/.bin/vsce package --no-dependencies --skip-license --target darwin-arm64 -o out/kilo-code-plus-7.3.54-v0.01-darwin-arm64.vsix
```

如果只修改 VS Code extension host 代码,且不涉及 CLI binary、SDK、webview 依赖,可用较快的本地验证方式:

```sh
cd /Users/czs/Developer/kilocode-plus/packages/kilo-vscode
../../node_modules/.bin/tsgo --noEmit
node esbuild.js --production
./node_modules/.bin/vsce package --no-dependencies --skip-license --target darwin-arm64 -o /tmp/kilo-code-plus-fix/kilo-code-plus-7.3.5401-darwin-arm64.vsix
```

注意:`vsce package` 必须带 `--no-dependencies --skip-license`,否则可能把 monorepo 上层文件错误打进 VSIX。

### 4.5 Cursor 安装测试

本地安装 VSIX 到 Cursor:

```sh
/Applications/Cursor.app/Contents/Resources/app/bin/cursor --install-extension /tmp/kilo-code-plus-fix/kilo-code-plus-7.3.5401-darwin-arm64.vsix --force
```

安装后在 Cursor 执行 `Developer: Reload Window`,或直接重启 Cursor。

验证点:

| 项 | 期望 |
|---|---|
| 扩展详情页 | 显示 `Kilo Code Plus` 和中文 Plus 说明 |
| 扩展版本 | 显示市场版本,例如 `7.3.5401` |
| 关于页面 | 版本信息不再显示 `unknown` |
| 自定义 provider | OpenAI / Anthropic / Gemini 模型发现、保存、图片能力、推理能力、token limit、成本选项正常 |
| 模型列表 | 默认只显示 Kilo 免费模型和用户已连接 provider 模型 |

---
