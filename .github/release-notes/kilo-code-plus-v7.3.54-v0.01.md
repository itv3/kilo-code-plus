# Kilo Code Plus 7.3.54-v0.01

市场内部版本: `7.3.5401`

这是 Plus 分支的首次自动发布批次,重点是发布元数据和自动化流程,不是主要功能新增批次。

## 主要变更

- 建立 `kilo-code-plus` 扩展元数据,用于和上游 `kilo-code` 区分。
- 新增 Plus 专用发布 workflow,开始支持通过 tag 自动发布 GitHub Release 和 Open VSX。
- 调整首发阶段的发布流水线,避免不适合 Plus 发布路径的 lint gate 阻断打包。
- 写入 Plus 市场版本和基础 README / CHANGELOG 信息。

## 安装包说明

该批次仍是早期发布验证包,GitHub Release 只上传了单个平台 `.vsix`。需要完整 macOS、Windows、Linux 平台包的用户请使用 `v0.04` 或更新版本。
