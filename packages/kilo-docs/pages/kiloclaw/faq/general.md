---
title: "FAQ"
description: "Frequently asked questions about KiloClaw"
---

# FAQ

## How can I change my model?

You can change the model in two ways:

- **From chat** — Type `/model` in the Chat window within the OpenClaw Control UI to switch models directly.
- **From the dashboard** — Go to [https://app.kilo.ai/claw](https://app.kilo.ai/claw), select the model you want, and click **Save**. No redeploy is needed.

## Can I access the filesystem?

You can access instance files in `/root/.openclaw/` directly from the [KiloClaw Dashboard](https://app.kilo.ai/claw). This is useful for examining or restoring config files. You can also interact with files through your OpenClaw agent using its built-in file tools.

## Can I access my KiloClaw via SSH?

For security reasons, SSH access is currently disabled for all KiloClaw instances. Our primary goal is to provide a secure environment for all users, and restricting direct SSH access is one of the many measures we take to ensure the platform remains safe and protected for everyone.

## How can I update my OpenClaw?

Do **not** click **Update Now** inside the OpenClaw Control UI — this is not supported for KiloClaw instances and may break your setup.

Updates are managed by the KiloClaw platform team to ensure stability. When a new version is available, it will be announced in the **Changelog** on your dashboard. To apply the update, click **Upgrade & Redeploy** from the [KiloClaw Dashboard](/docs/kiloclaw/dashboard#redeploy).

## How do I migrate my workspace to a new instance?

When switching from one KiloClaw instance to another — or to a different OpenClaw provider — you can migrate your Claw's workspace (including memory and other files) by transferring the workspace directory. Integrations you've configured (Google, GitHub, GitLab, Linear, Telegram, etc.) will need to be set up again manually on the new instance.

### What migrates and what doesn't?

- **Migrates** — Workspace files, memory, context, and any data stored in the workspace directory.
- **Does not migrate** — Integrations and authentication tokens (Google Workspace, GitHub, GitLab, Linear, Telegram, Discord, Slack, etc.). These must be reconfigured on the new instance.

{% callout type="info" %}
Your Claw's workspace memory and context carry over with the workspace files, so your new instance will retain the same knowledge and preferences as before.
{% /callout %}

### Backing up your workspace

The recommended approach is to have your Claw save its workspace externally, then restore it on the new instance. There are two options:

#### Option 1: Back up to GitHub

Ask your Claw to push the workspace to a GitHub repository:

> Create a new GitHub repo and push your entire workspace there with the `gh` CLI. Tell me the URL of the repo you used.

#### Option 2: Manual export (Google Drive)

Ask your Claw to compress and upload the workspace to Google Drive:

> Tar compress your workspace and push the file to Google Drive with the `gog` CLI. Then share the filename you used.

### Restoring your workspace

Once you have a backup, start your new KiloClaw instance and ask the Claw to restore the workspace.

#### Restore from GitHub

> The GitHub repo `<repo>` has a backup of your workspace in it. Please pull the workspace from the repo with the `gh` CLI and overwrite the existing workspace directory with the repo's contents.

#### Restore from Google Drive

> The Google Drive file `<filename>` has a backup of your workspace in it. Please pull the tar file from Google Drive with the `gog` CLI and overwrite the existing workspace directory with its contents.

{% callout type="note" %}
Replace `<repo>` or `<filename>` with the actual repository URL or filename from the backup step.
{% /callout %}

### Reconfiguring integrations after migration

After restoring your workspace, you will need to reconfigure each integration manually:

1. **Google Workspace** — Re-authenticate with Google. See [Google integration setup](/docs/kiloclaw/development-tools/google).
2. **GitHub** — Re-authenticate with GitHub. See [GitHub integration setup](/docs/kiloclaw/development-tools/github).
3. **GitLab** — Re-add your GitLab token or SSH key.
4. **Linear** — Re-add your Linear API token.
5. **Telegram / Discord / Slack** — Re-enter your bot tokens in the [KiloClaw Dashboard Settings](/docs/kiloclaw/dashboard#channels) and redeploy.

See [Development Tools](/docs/kiloclaw/development-tools) and [Chat Platforms](/docs/kiloclaw/chat-platforms) for full setup instructions.

{% callout type="warning" %}
Workspace migration transfers files only. Integration tokens and OAuth sessions are not included in the workspace directory and must be set up fresh on the new instance.
{% /callout %}
