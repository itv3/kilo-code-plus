# Kilo Code Plus

这是基于上游 Kilo Code 的 Plus 版本，主要改进如下。

## 版本说明

- 上游版本：`7.3.54`
- Plus 自定义版本：`v0.01`
- 发布批次：`7.3.54-v0.01`
- 市场版本：`7.3.5401`

说明：VS Marketplace 不支持 `7.3.54-v0.01` 这种带后缀的扩展版本号，所以市场页面显示的版本使用纯数字 `7.3.5401`。GitHub tag、GitHub Release、VSIX 文件名使用发布批次 `7.3.54-v0.01`。

## 主要改进

### UI 页面自定义提供商增强

1. API 类型：自定义提供商支持 OpenAI / Anthropic / Gemini 三种 API 类型。
2. 模型自动发现：支持 OpenAI、Anthropic、Gemini，并自动处理 endpoint 和认证头。
3. 模型能力配置：支持手动勾选图像输入能力、推理能力。
4. 高级模型参数：支持 context token limit、output token limit。
5. 成本选项：默认隐藏，勾选后显示和保存输入、输出、缓存读取、缓存写入成本。
6. 默认值补全：添加模型时用模型 ID 匹配内置默认模型，匹配上后自动带出高级模型参数、图像输入能力、推理能力、成本。

## 发布

自动发布 workflow：`.github/workflows/publish-kilo-code-plus.yml`

推送 tag 后自动发布到：

- Open VSX：`https://open-vsx.org/extension/itv3/kilo-code-plus`
- VS Marketplace：`https://marketplace.visualstudio.com/items?itemName=itv3.kilo-code-plus`

当前发布目标平台：`darwin-arm64`

常用发布命令：

```bash
git push custom main
git tag kilo-code-plus-v7.3.54-v0.01
git push custom kilo-code-plus-v7.3.54-v0.01
```

## 反馈

如果是上游 Kilo Code 本身的源码问题，请向上游反馈。

如果是 Plus 自定义提供商增强相关问题，请在本仓库反馈。
