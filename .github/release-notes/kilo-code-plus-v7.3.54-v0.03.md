# Kilo Code Plus 7.3.54-v0.03

市场内部版本: `7.3.5403`

这是 Plus 功能收口版,集中补齐自定义提供商、模型列表过滤和智能体中文化。

## 主要变更

- 增强自定义提供商 UI:支持 OpenAI、Anthropic、Gemini 三种 API 类型。
- 支持 OpenAI、Anthropic、Gemini 模型自动发现,并处理不同 endpoint 和认证头。
- 添加模型时按模型 ID 匹配内置默认模型,自动带出 token limit、图像输入、推理能力和成本默认值。
- 支持手动配置图像输入能力、推理能力、context token limit、output token limit 和成本选项。
- 调整聊天窗口模型列表:默认只显示 Kilo 免费模型,用户已连接或自行添加的 provider 模型继续显示。
- 翻译设置页和聊天窗口中的内置智能体描述,并从可见列表隐藏已弃用的 `orchestrator`。
- 修复 Plus 扩展 ID 改名后的版本读取和扩展信息显示问题。
- 加固自定义 provider 保存、模型发现、重定向处理、i18n 校验和相关单元测试。

## 安装包说明

该批次仍是单平台发布验证包。需要完整 macOS、Windows、Linux 平台包的用户请使用 `v0.04` 或更新版本。
