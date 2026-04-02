# AGENTS.md — Kilo JetBrains Plugin

## Architecture (Split Mode)

- **Split-mode plugin** with three Gradle modules: `shared/`, `frontend/`, `backend/`. The module descriptors are `kilo.jetbrains.shared.xml`, `kilo.jetbrains.frontend.xml`, `kilo.jetbrains.backend.xml` — these must stay in sync with `plugin.xml`'s `<content>` block.
- Reference template for the split-mode structure: https://github.com/nicewith/intellij-platform-modular-plugin-template
- Official docs: https://plugins.jetbrains.com/docs/intellij/split-mode-for-remote-development.html
- Kotlin source goes under `{module}/src/main/kotlin/ai/kilocode/jetbrains/`. Package name is `ai.kilocode.jetbrains` (matches `group` in root `build.gradle.kts`).
- **Module placement rules**: backend modules host project model, indexing, analysis, execution, and CLI process management. Frontend modules host UI, typing assistance, and latency-sensitive features. Shared modules define RPC interfaces and data types used by both sides.
- In monolithic IDE mode (non-remote), all three modules load in one process — split plugins work fine without remote dev.
- Frontend ↔ backend communication uses RPC interfaces defined in `shared/`. Data sent over RPC must use `kotlinx.serialization`. In monolithic mode RPC is just an in-process suspend call.
- **Testing split mode**: run `./gradlew generateSplitModeRunConfigurations` to create a "Run IDE (Split Mode)" config that starts both frontend and backend processes locally. Emulate latency via the Split Mode widget (requires internal mode: `-Didea.is.internal=true`).

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

- Official docs: https://plugins.jetbrains.com/docs/intellij/plugin-services.html and https://plugins.jetbrains.com/docs/intellij/launching-coroutines.html
- **Prefer light services**: annotate with `@Service` (or `@Service(Service.Level.PROJECT)`) instead of registering in XML when the service won't be overridden or exposed as API. Light services must be `final` in Java (no `open` in Kotlin), cannot use constructor injection of other services, and don't support `os`/`client`/`overrides` attributes.
- Non-light services that need XML registration go in `kilo.jetbrains.backend.xml` under `<extensions defaultExtensionNs="com.intellij"><applicationService>` (or `<projectService>`).
- **Constructor-injected `CoroutineScope`**: the recommended way to launch coroutines. Each service gets its own scope (child of an intersection scope). The scope is cancelled on app/project shutdown or plugin unload. Supported signatures: `MyService(CoroutineScope)` for app services, `MyService(Project, CoroutineScope)` for project services.
- The injected scope's context contains `Dispatchers.Default` and `CoroutineName(serviceClass)`. Switch to `Dispatchers.IO` for blocking I/O.
- **Avoid heavy constructor work** — defer initialization to methods. Never cache service instances in fields; always retrieve via `service<T>()` at the call site.
- `runBlockingCancellable` exists but is **not recommended** — use service scopes instead. For actions, use `currentThreadCoroutineScope()` which lets the Action System cancel the coroutine.
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
