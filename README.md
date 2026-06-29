# Kilo Code Plus

这是基于上游 Kilo Code 的 Plus 版本，主要改进如下。

## 版本说明

- 上游版本：`7.3.54`
- Plus 自定义版本：`v0.03`
- 发布批次：`7.3.54-v0.03`
- 市场版本：`7.3.5403`

说明：VS Marketplace 不支持 `7.3.54-v0.03` 这种带后缀的扩展版本号，所以市场页面显示的版本使用纯数字 `7.3.5403`。GitHub tag、GitHub Release、VSIX 文件名使用发布批次 `7.3.54-v0.03`。

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

### 模型列表过滤与排序

默认仅显示 Kilo Gateway 免费模型以及用户已添加或已连接提供商的模型，减少无关付费模型干扰。聊天窗口中，用户添加或连接的提供商模型会排在免费模型前面，便于优先选择自己的模型。

### 智能体中文化与隐藏 orchestrator

补齐设置页面和聊天窗口中内置智能体的中文显示说明，并从可见列表隐藏已弃用的 `orchestrator` 智能体，保留用户自定义智能体的显示行为。

## 手动安装

如果暂时不通过扩展市场安装,可以直接下载 GitHub Release 中的 `.vsix` 文件后手动安装。

下载地址: `https://github.com/itv3/kilo-code-plus/releases`

### Cursor

方式一:在 Cursor 中打开命令面板,执行 `Extensions: Install from VSIX...`,选择下载好的 `.vsix` 文件。

方式二:使用命令行安装:

```bash
/Applications/Cursor.app/Contents/Resources/app/bin/cursor --install-extension /path/to/kilo-code-plus-7.3.54-v0.03-darwin-arm64.vsix --force
```

### VS Code

方式一:在 VS Code 中打开命令面板,执行 `Extensions: Install from VSIX...`,选择下载好的 `.vsix` 文件。

方式二:使用命令行安装:

```bash
code --install-extension /path/to/kilo-code-plus-7.3.54-v0.03-darwin-arm64.vsix --force
```

安装完成后执行 `Developer: Reload Window`,或重启编辑器。

## 发布

自动发布 workflow：`.github/workflows/publish-kilo-code-plus.yml`

推送 tag 后自动发布到：

- Open VSX：`https://open-vsx.org/extension/itv3/kilo-code-plus`
- VS Marketplace：`https://marketplace.visualstudio.com/items?itemName=itv3.kilo-code-plus`

当前发布目标平台：`darwin-arm64`

常用发布命令：

```bash
git push custom main
git tag kilo-code-plus-v7.3.54-v0.03
git push custom kilo-code-plus-v7.3.54-v0.03
```

## 反馈

如果是上游 Kilo Code 本身的源码问题，请向上游反馈。

如果是 Plus 自定义提供商增强相关问题，请在本仓库反馈。
