# Kilo Code Plus

这是基于上游 Kilo Code 的 Plus 版本，主要改进如下。

## 版本说明

- 上游版本：`7.3.54`
- Plus 自定义版本：`v0.05`
- 发布批次：`7.3.54-v0.05`
- 市场版本：`7.3.5405`

说明：VS Marketplace 不支持 `7.3.54-v0.05` 这种带后缀的扩展版本号，所以市场页面显示的版本使用纯数字 `7.3.5405`。GitHub tag、GitHub Release、VSIX 文件名使用发布批次 `7.3.54-v0.05`。

## 主要改进

### UI 页面自定义提供商增强

| 功能 | 说明 |
|---|---|
| API 格式 | 自定义提供商支持 OpenAI / Anthropic / Gemini 三种原生格式 API。 |
| 模型自动发现 | 支持 OpenAI、Anthropic、Gemini，并自动处理 endpoint 和认证头。 |
| 模型能力配置 | 支持添加图像输入能力、推理能力。 |
| 高级模型参数 | 支持 `context token limit`、`output token limit`。 |
| 成本选项 | 支持输入、输出、缓存读取、缓存写入成本。 |
| 默认值补全 | 添加模型时用模型 ID 匹配内置默认模型，匹配上后自动带出高级模型参数、图像输入能力、推理能力、成本。 |

### 模型列表过滤、排序与选择体验

默认仅显示 Kilo Gateway 免费模型以及用户已添加或已连接提供商的模型，减少无关付费模型干扰。聊天窗口中，用户添加或连接的提供商模型会排在免费模型前面；模型选择器默认折叠详情页，单击模型即可直接切换。首次没有收藏记录时，默认收藏 StepFun Step 3.7 Flash 免费模型，用户取消后不会自动恢复。

### 智能体中文化与隐藏 orchestrator

补齐设置页面和聊天窗口中内置智能体的中文显示说明，并从可见列表隐藏已弃用的 `orchestrator` 智能体，保留用户自定义智能体的显示行为。

## 手动安装

如果暂时不通过扩展市场安装,可以直接下载 GitHub Release 中的 `.vsix` 文件后手动安装。Cursor 和 VS Code 的安装流程一样。

下载地址: `https://github.com/itv3/kilo-code-plus/releases`

安装步骤:

1. 从 GitHub Release 下载与你系统匹配的 `.vsix` 文件。
2. 打开 Cursor 或 VS Code。
3. 打开命令面板:Windows / Linux 按 `Ctrl+Shift+P`,macOS 按 `Cmd+Shift+P`。
4. 输入并执行 `Extensions: Install from VSIX...`,选择下载好的 `.vsix` 文件。
5. 安装完成后执行 `Developer: Reload Window`,或重启编辑器。

`.vsix` 文件区分系统和 CPU 架构,请按下面的包名选择:

| 包名 | 对应环境 | 常见用户 |
|---|---|---|
| `darwin-arm64` | macOS + Apple Silicon | M1 / M2 / M3 / M4 Mac |
| `darwin-x64` | macOS + Intel | 老款 Intel Mac |
| `win32-x64` | Windows + x86_64 | 绝大多数 Windows 电脑 |
| `win32-arm64` | Windows + ARM64 | Surface Pro ARM、骁龙 X Elite Windows 电脑 |
| `linux-x64` | Linux + x86_64 | 绝大多数 Linux 桌面 / 服务器 |
| `linux-arm64` | Linux + ARM64 | ARM 服务器、树莓派、部分 ARM Linux 设备 |

包名使用 VS Code 官方 `targetPlatform` 标识,所以 macOS 对应的是 `darwin`,不能改成 `mac`。通过 Open VSX 安装时,市场会在已发布的平台包中按当前环境选择匹配包;手动安装时需要自己下载对应系统的 `.vsix`。

## 发布

自动发布 workflow：`.github/workflows/publish-kilo-code-plus.yml`

推送 tag 后自动发布到：

- Open VSX：`https://open-vsx.org/extension/itv3/kilo-code-plus`
- GitHub Release：`https://github.com/itv3/kilo-code-plus/releases`

当前发布目标平台：`darwin-arm64`、`darwin-x64`、`win32-x64`、`win32-arm64`、`linux-x64`、`linux-arm64`。每次正式发布都应同时上传这 6 个 `.vsix` 文件,确保 macOS、Windows、Linux 用户都能安装。

标准发布流程：

```bash
mkdir -p .github/release-notes
$EDITOR .github/release-notes/kilo-code-plus-v7.3.54-v0.05.md
cd packages/kilo-vscode
bun test ./tests/unit/custom-provider-defaults.test.ts ./tests/unit/custom-provider-dialog-validate.test.ts
bun run typecheck
cd ../..
git push custom main
git tag kilo-code-plus-v7.3.54-v0.05
git push custom kilo-code-plus-v7.3.54-v0.05
```

发布前必须为本次 tag 准备 `.github/release-notes/<tag>.md`,逐条写清具体变更。发布后先看 GitHub Actions 中 `publish-kilo-code-plus` workflow 是否成功,再核对 GitHub Release 是否有 6 个平台 `.vsix`。Open VSX 发布成功后通常需要几分钟后台传播,不要立刻按旧版本查询结果判断失败;等待后用 `targetPlatform` 查询下载映射,确认 6 个平台都指向新市场版本。

## 反馈

如果是上游 Kilo Code 本身的源码问题，请向上游反馈。

如果是 Plus 自定义提供商增强相关问题，请在本仓库反馈。
