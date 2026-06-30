# Kilo Code Plus 7.3.54-v0.04

市场内部版本: `7.3.5404`

这是当前推荐版本,重点修复自定义 provider 模型变体默认值匹配,并补齐全平台发布包。

## 主要变更

- 自定义 provider 添加模型时,支持把 `claude-opus-4-6-thinking`、`claude-opus-4-6-reasoning` 这类后缀模型匹配到内置默认模型 `claude-opus-4-6`。
- 匹配到内置默认模型后,自动继承价格、推理能力、图像输入能力、context token limit、output token limit 和成本默认值。
- 增加模型变体默认值匹配的单元测试,避免后续回归。
- 更新手动安装说明,明确 `.vsix` 按操作系统和 CPU 架构区分。
- 调整 Plus 发布 workflow:通过 Bun 构建 CLI,tag 发布默认跳过 VS Marketplace,稳定发布到 Open VSX 和 GitHub Release。
- GitHub Release 和 Open VSX 均发布 6 个平台包:`darwin-arm64`、`darwin-x64`、`win32-x64`、`win32-arm64`、`linux-x64`、`linux-arm64`。

## 安装包说明

本批次已补齐 macOS、Windows、Linux 的 6 个平台 `.vsix`。通过 Open VSX 安装时市场会按当前环境选择匹配包;手动安装时请下载对应系统和架构的文件。
