---
description: Resolve upstream merge conflicts
---

Resolve the manual part of an upstream merge.

Arguments: `$ARGUMENTS`

Use the first argument as the upstream version, for example `v1.1.50` or
`1.1.50`. If no argument is provided, infer the version from the current branch
name, `upstream-merge-report-<version>.md`, or the newest relevant report file.

Workflow:

1. Inspect the current merge state:
   - `git status --short`
   - `git diff --name-only --diff-filter=U`
   - `upstream-merge-report-<version>.md` when present
   - `.worktrees/opencode-merge/auto-merge` for the automated merge snapshot when present
2. Before editing, write a concise plan in the chat:
   - file-by-file strategy
   - expected resolution kind: `hybrid`, `take-ours`, `take-theirs`, `regenerated`, `removed`, `renamed`, or `other`
   - risk level: `low`, `medium`, or `high`
   - verification commands you expect to run
3. Resolve each conflict carefully.

   **Reference worktrees when present:**
   - `.worktrees/opencode-merge/opencode` is the pristine upstream opencode tree
   - `.worktrees/opencode-merge/kilo-main` is the Kilo base snapshot
   - `.worktrees/opencode-merge/auto-merge` is the automated merge snapshot and the original conflict reference

   **Inspect conflicts (optional):**
   - `script/upstream/find-conflict-markers.sh <file>` on the working tree
   - `script/upstream/find-conflict-markers.sh .worktrees/opencode-merge/auto-merge/<file>` on the auto-merge snapshot

   **Apply the resolution rules:**
   - prefer upstream code and architecture whenever compatible with Kilo behavior
   - preserve Kilo-specific behavior marked with `kilocode_change`
   - keep `kilocode_change` markers around Kilo-specific changes in shared opencode files
   - keep Kilo-specific text, code, and marker comments the same as the auto-merge conflict snapshot unless a refactor is required
   - if Kilo-specific code must be refactored to fit new upstream architecture, explain the refactor in the final summary
   - if upstream moved the relevant logic to another file, port the Kilo behavior there and list both paths in the final summary
   - if upstream deleted a file, analyze whether the Kilo behavior should be ported elsewhere or removed rather than restoring the deleted file
   - if tests fail only because upstream intentionally removed behavior, remove or update the obsolete tests rather than adding the old file back
   - do not modify unrelated files
4. Run the appropriate checks:
   - stage resolved files with `git add -A` so git no longer reports unmerged paths
   - if `packages/opencode/` shared files changed, run `bun run script/check-opencode-annotations.ts`
   - run targeted typechecks/tests when practical for touched packages
   - run `bun run typecheck` from the repo root before declaring the merge ready
5. Finish with:
   - files resolved
   - resolution choices and rationale
   - checks run and results
   - any remaining high-risk areas for reviewer attention

Only ask the user before proceeding if a decision is destructive, changes auth,
billing, data deletion, public API compatibility, config schema behavior,
migrations, provider routing, or security posture in a way that cannot be
safely inferred from the existing Kilo changes.

Read `script/upstream/README.md` -> `Common Pitfalls` before resolving. Watch
for auto-merged code referencing conflict-block declarations, related sibling
files that need edits but are not unmerged, when to prefer `hybrid` over
`renamed`, function signatures drifting across a conflict boundary, and why
full turbo typecheck is the right catch-all.
