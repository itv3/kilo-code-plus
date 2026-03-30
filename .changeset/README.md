# Changesets

This directory contains changeset files used to track changes for the next release.

## Adding a changeset

When making a user-facing change, run:

```sh
bunx changeset add
```

Or manually create a file `.changeset/<slug>.md`:

```md
---
"kilo-code": minor
---

Short description of the change for the changelog.
```

Use `patch` for bug fixes, `minor` for new features, `major` for breaking changes.

Changeset files are consumed when the version bump PR is merged, updating `CHANGELOG.md` and bumping `package.json` versions.
