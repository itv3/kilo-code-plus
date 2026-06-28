# Kilo Code Plus

这是基于上游 Kilo Code 的 Plus 版本，主要增强自定义提供商能力。

发布批次：`7.3.54-v0.01`

市场版本：`7.3.5401`

含义：基于上游 Kilo Code `7.3.54`，Plus 自定义版本 `v0.01`。

## 主要改进

### UI 页面自定义提供商增强

1. API 类型：自定义提供商支持 OpenAI / Anthropic / Gemini 三种 API 类型。
2. 模型自动发现：支持 OpenAI、Anthropic、Gemini，并自动处理 endpoint 和认证头。
3. 模型能力配置：支持手动勾选图像输入能力、推理能力。
4. 高级模型参数：支持 context token limit、output token limit。
5. 成本选项：默认隐藏，勾选后显示和保存输入、输出、缓存读取、缓存写入成本。
6. 默认值补全：添加模型时用模型 ID 匹配内置默认模型，匹配上后自动带出高级模型参数、图像输入能力、推理能力、成本。

## 说明

Kilo Code Plus 尽量保持最小入侵，主要改动集中在 VS Code 扩展侧，方便后续跟随上游升级。

如果是上游 Kilo Code 本身的源码问题，请向上游反馈。

如果是 Plus 自定义提供商增强相关问题，请在本仓库反馈。
