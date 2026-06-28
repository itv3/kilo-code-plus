# kilo code-custom · 开发方案

## 0. 目标与结论

基于 Kilo Code v2 做一个自定义发行版:

1. **自定义提供商增强**:支持 OpenAI / Anthropic / Gemini 三种 API 类型;模型自动发现;模型能力与高级参数配置;成本选项;添加模型时按模型 ID 匹配内置默认值。
2. **模型列表过滤**:默认只显示 Kilo 自带免费模型;用户后续添加的模型继续显示。
3. **智能体中文化**:补齐 UI 中文,移除冗余的 `orchestrator` 智能体。
4. **自动发布**:跟随上游 merge + tag,CI 自动发布到 Open VSX 和微软 Marketplace。

结论:四项均可行,整体工作量 **中**。第一项已完成实现和本地 Cursor 安装验证;后续主要改动集中在 provider 列表过滤、少量 i18n / agent 配置和发布配置。

基础约束:

| 项 | 结论 |
|---|---|
| 产品名 | `kilo code-custom` |
| 目标编辑器 | Cursor + VS Code |
| 工程基础 | Kilo Code v2 `main` / `master` |
| 内部命令 ID | 保留 `kilo-code.*` / `kilo-code-ActivityBar`,避免命令和视图失效 |
| 维护方式 | 定期 merge 上游,打 tag 后自动发布 |

最小入侵边界:

1. 自定义 provider 增强尽量只改 `packages/kilo-vscode`,不碰 `packages/opencode/src/provider/provider.ts`;底层已经支持 `@ai-sdk/google`、`modalities`、`limit`。
2. 智能体中文化只做 UI 展示映射,不改 agent 内部 `name`,不翻系统 prompt。
3. `orchestrator` 不做大范围删除;优先在展示/可选列表里隐藏,或在 Kilo 自有 agent 注入层禁用。

---

## 1. 需求一 · 自定义提供商增强

### 1.1 已实现功能

| 功能 | 说明 |
|---|---|
| API 类型 | 自定义提供商支持 OpenAI / Anthropic / Gemini 三种 API 类型 |
| 模型自动发现 | 支持 OpenAI、Anthropic、Gemini,并自动处理 endpoint 和认证头 |
| 模型能力配置 | 支持手动勾选图像输入能力、推理能力 |
| 高级模型参数 | 支持 `context token limit`、`output token limit` |
| 成本选项 | 默认隐藏,勾选后显示和保存输入、输出、缓存读取、缓存写入成本 |
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

目标行为:

| 用户配置状态 | 模型列表显示 |
|---|---|
| 未添加任何 provider 或自定义模型 | Kilo 自带免费模型 |
| 添加 OpenAI 订阅 | Kilo 自带免费模型 + OpenAI 订阅模型 |
| 再添加中转站 | Kilo 自带免费模型 + OpenAI 订阅模型 + 中转站模型 |

过滤点:扩展端 `fetchAndSendProviders()` 中,在 `indexProvidersById(response.all)` 前处理 `response.all`。

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

注意:不要用 `provider.source === "custom"` 判断用户自定义添加。models.dev 目录导入的普通 provider 也可能带 `source: "custom"`,会误放出未添加模型。

关键文件:

| 用途 | 文件 |
|---|---|
| 过滤点 | `packages/kilo-vscode/src/KiloProvider.ts` |
| provider 数据 | `packages/kilo-vscode/src/provider-actions.ts` |
| `connected` 生成 | `packages/opencode/src/server/routes/instance/httpapi/handlers/provider.ts` |

待验证:`response.connected` 是否覆盖 OpenAI 订阅、API key provider、OpenAI-compatible / Anthropic / Gemini 自定义 provider。

---

## 3. 需求三 · 智能体中文化与移除 orchestrator

中文化范围以“智能体行为 > 代理”页面和 agent picker 的可见列表为准,也就是图中这类英文展示项:

- `ask` / `code` / `debug` / `explore` / `general` / `plan` 的显示名;
- 每个智能体下面的英文描述;
- `子代理`、`弃用` 等状态标签和相关 UI 文案。

不翻译系统提示词。

| 层 | 处理 |
|---|---|
| 智能体列表显示 | 翻译名称、描述、状态标签 |
| 设置页/选择器 UI | 翻译按钮、标题、说明文字 |
| 系统提示词 | 不翻 |

系统提示词影响模型行为,且上游可能频繁优化;翻译会增加冲突和效果风险。

`orchestrator` 处理方式:通过 agent 配置里的 `disable` 禁用,不删除核心定义。

关键文件:

| 用途 | 文件 |
|---|---|
| agent 注入/disable | `packages/opencode/src/agent/agent.ts` |
| orchestrator 定义 | `packages/opencode/src/kilocode/agent/index.ts` |
| 冗余说明 | `packages/opencode/src/kilocode/docs/migration.md` |
| 中文文案 | `packages/kilo-vscode/webview-ui/src/i18n/zh.ts` |
| Agent Manager 中文 | `packages/kilo-vscode/webview-ui/agent-manager/i18n/zh.ts` |

不要碰 `packages/kilo-indexing/src/indexing/orchestrator.ts`;它是代码索引编排器。

---

## 4. 需求四 · 扩展标识与自动发布

扩展标识:

| 字段 | 建议值 |
|---|---|
| `displayName` | `kilo code-custom` |
| `name` | `kilo-code-custom` |
| `publisher` | 待定 |
| `icon` | 自定义图标 |
| `repository.url` | fork 仓库 |

不要改 `contributes` 里的 `kilo-code.*` 前缀。

自动发布:

| 任务 | 文件/配置 |
|---|---|
| 修改 fork 仓库限制 | `.github/workflows/publish.yml` |
| 配置 Marketplace token | `VSCE_TOKEN` |
| 配置 Open VSX token | `OPENVSX_TOKEN` |
| 打包脚本 | `packages/kilo-vscode/script/build.ts` |
| 发布脚本 | `packages/kilo-vscode/script/publish.ts` |

---

## 5. 工作量与风险

| 模块 | 最小改动 | 风险 |
|---|---|---|
| 自定义 provider 增强 | 已完成,涉及 `packages/kilo-vscode` 下 provider 保存、模型发现、设置 UI、message 类型、i18n、单测 | 中 |
| 模型过滤 | `KiloProvider.ts` | 中 |
| 智能体中文化 | `zh.ts`、agent 配置 | 中 |
| 扩展标识与发布 | `package.json`、`constants.ts`、README、图标、`publish.yml` | 中 |

第一阶段自定义 provider 增强已涉及 **15 个文件**。后续主要冲突点是 `KiloProvider.ts`、`zh.ts`、`package.json`、`publish.yml`。

---

## 6. 待确认与执行顺序

已确认:

| 编号 | 事项 |
|---|---|
| C1 | Gemini Native 默认 base URL 和模型发现 URL 拼接已实现 |
| C2 | OpenAI / Anthropic / Gemini 模型发现已通过当前中转站测试 |
| C3 | token limit 已作为模型卡片高级参数保存 |

待确认:

| 编号 | 事项 |
|---|---|
| C4 | `response.connected` 是否覆盖所有用户添加模型路径 |
| C5 | 是否只翻 UI 层中文 |
| C6 | `publisher`、图标、市场页文案 |

执行顺序:

| 阶段 | 内容 |
|---|---|
| 0 | fork 仓库、本地构建、安装基线 vsix |
| 1 | 自定义 provider 增强,已完成 |
| 2 | 模型列表过滤 |
| 3 | 禁用 `orchestrator`、补齐 UI 中文 |
| 4 | 扩展标识与发布配置 |
| 5 | 端到端验证 |
