# AGENTS.md — Kilo JetBrains Plugin

## Architecture

- **Split-mode plugin** with three Gradle modules: `shared/`, `frontend/`, `backend/`. The module descriptors are `kilo.jetbrains.shared.xml`, `kilo.jetbrains.frontend.xml`, `kilo.jetbrains.backend.xml` — these must stay in sync with `plugin.xml`'s `<content>` block.
- Reference template for the split-mode structure: https://github.com/nicewith/intellij-platform-modular-plugin-template
- Kotlin source goes under `{module}/src/main/kotlin/ai/kilocode/jetbrains/`. No Kotlin source existed before this service was added — the modules were XML-only scaffolding.
- Package name is `ai.kilocode.jetbrains` (matches `group` in root `build.gradle.kts`).

## CLI Binary Bundling

- CLI binaries are bundled as **JAR resources** at `/cli/{platform}/kilo[.exe]` where platform is `darwin-arm64`, `darwin-x64`, `linux-arm64`, `linux-x64`, `windows-x64`, `windows-arm64`.
- The build script (`script/build.ts`) copies binaries from `packages/opencode/dist/` into `backend/build/generated/cli/cli/{platform}/`. Gradle includes this via `sourceSets.main.resources.srcDir`.
- `./gradlew buildPlugin` requires CLI binaries to already exist in `backend/build/generated/cli/`. Run `bun run build` first (which calls the build script + Gradle), or ensure binaries are present before invoking Gradle directly.
- Production builds (`bun run build:production` or `./gradlew buildPlugin -Pproduction=true`) require all 6 platform binaries. Local builds only need the current platform.

## CLI Extraction at Runtime

- Binaries are extracted to `PathManager.getSystemPath()/kilo/bin/kilo` — this is the standard JetBrains location for plugin-extracted native binaries (e.g. `~/Library/Caches/JetBrains/IntelliJIdea2025.1/kilo/bin/kilo` on macOS).
- Use `com.intellij.util.system.CpuArch.CURRENT` for architecture detection, not `System.getProperty("os.arch")`. `CpuArch` is an enum (`X86_64`, `ARM64`, etc.) and handles Rosetta/WoW64 detection.
- Use `com.intellij.openapi.util.SystemInfo.isMac`/`isLinux`/`isWindows` for OS detection.

## Services and Coroutines

- Backend services use constructor-injected `CoroutineScope` per JetBrains docs. The scope is cancelled automatically on application shutdown or plugin unload.
- Application-level services (`Service.Level.APP`) are registered in `kilo.jetbrains.backend.xml` under `<extensions defaultExtensionNs="com.intellij"><applicationService>`.
- No extra coroutines dependency is needed — `kotlinx.coroutines` is bundled by the IntelliJ platform and available transitively.

## CLI Server Protocol

- The plugin spawns `kilo serve --port 0` (OS assigns random port) and reads stdout for `listening on http://...:(\d+)` to discover the port.
- A random 32-byte hex password is passed via `KILO_SERVER_PASSWORD` env var for Basic Auth.
- Key env vars: `KILO_CLIENT=jetbrains`, `KILO_PLATFORM=jetbrains`, `KILO_APP_NAME=kilo-code`, `KILO_ENABLE_QUESTION_TOOL=true`.
- This is the same protocol used by the VS Code extension (`packages/kilo-vscode/src/services/cli-backend/server-manager.ts`).

## Build

- **Full build**: `bun run build` from `packages/kilo-jetbrains/` (builds CLI + Gradle plugin).
- **Gradle only**: `./gradlew buildPlugin` from `packages/kilo-jetbrains/` (requires CLI binaries already present).
- **Via Turbo**: `bun turbo build --filter=@kilocode/kilo-jetbrains` from repo root.
- **Run in sandbox**: `./gradlew runIde` — launches sandboxed IntelliJ with the plugin. Does NOT build CLI binaries.

## Files That Must Change Together

- `plugin.xml` `<content>` entries ↔ module XML descriptors (`kilo.jetbrains.{shared,frontend,backend}.xml`)
- Service classes ↔ `<applicationService>`/`<projectService>` entries in the corresponding module XML
- `script/build.ts` platform list ↔ `backend/build.gradle.kts` `requiredPlatforms` list
