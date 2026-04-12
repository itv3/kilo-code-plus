plugins {
    alias(libs.plugins.rpc)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
}

kotlin {
    jvmToolchain(21)
}

val generatedApi = layout.buildDirectory.dir("generated/openapi/src/main/kotlin")

sourceSets {
    main {
        resources.srcDir(layout.buildDirectory.dir("generated/cli"))
        kotlin.srcDir(generatedApi)
    }
}

openApiGenerate {
    generatorName.set("kotlin")
    library.set("jvm-okhttp4")
    inputSpec.set("${rootDir}/../sdk/openapi.json")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    packageName.set("ai.kilocode.jetbrains.api")
    apiPackage.set("ai.kilocode.jetbrains.api.client")
    modelPackage.set("ai.kilocode.jetbrains.api.model")
    configOptions.set(mapOf(
        "serializationLibrary" to "kotlinx_serialization",
        "omitGradleWrapper" to "true",
        "omitGradlePluginVersions" to "true",
        "useCoroutines" to "false",
        "sourceFolder" to "src/main/kotlin",
        "enumPropertyNaming" to "UPPERCASE",
    ))
    // Remap schema "File" so the generated class is not named java.io.File
    modelNameMappings.set(mapOf(
        "File" to "DiffFileInfo",
    ))
    // Map empty anyOf references to kotlin.Any; bare numbers to Double
    typeMappings.set(mapOf(
        "AnyOfLessThanGreaterThan" to "kotlin.Any",
        "anyOf<>" to "kotlin.Any",
        "number" to "kotlin.Double",
        "decimal" to "kotlin.Double",
    ))
    // Normalise OpenAPI 3.1 → 3.0-compatible patterns
    openapiNormalizer.set(mapOf(
        "SIMPLIFY_ANYOF_STRING_AND_ENUM_STRING" to "true",
        "SIMPLIFY_ONEOF_ANYOF" to "true",
    ))
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
}

// Fix openapi-generator codegen bugs in generated Kotlin sources.
//
// 1) Boolean const enum fix:
//    `const: true`/`const: false` fields produce broken single-value enums.
//    Fix: replace with kotlin.Boolean, remove the enum class.
//
// 2) Double-parentheses on HashMap-extending data classes:
//    `data class Foo(...) : HashMap<String, Any>()()` — extra `()`.
//    Fix: remove the trailing `()`.
//
// 7) Empty anyOf wrapper classes:
//    anyOf unions of heterogeneous types (e.g. string enum | object) generate
//    empty `class Foo () {}` that can't deserialize primitives.
//    Fix: replace references with JsonElement, delete the empty class files.
//
// 3) Private Double constructor:
//    `kotlin.Double("5000")` — Double has no public String constructor.
//    Fix: convert to `5000.0` double literal.
//
// 4) Missing @Contextual on bare kotlin.Any fields:
//    kotlinx.serialization can't serialize `Any` without @Contextual.
//    Fix: add @Contextual annotation where missing.
//
// 5) Nullable body access in ApiClient.kt:
//    `response.body` is nullable in OkHttp but generated code dereferences
//    it without safe calls. Fix: replace `body.` with `body?.`.
val fixGeneratedApi by tasks.registering {
    dependsOn("openApiGenerate")
    val dir = generatedApi
    doLast {
        // ── Fix 7: empty anyOf wrapper classes → JsonElement ────────
        // These are anyOf unions (e.g. string enum | object) that the
        // codegen produces as empty classes. Replace all references with
        // kotlinx.serialization.json.JsonElement and delete the files.
        val modelDir = dir.get().file("ai/kilocode/jetbrains/api/model").asFile
        val emptyWrappers = modelDir.listFiles()
            ?.filter { it.extension == "kt" }
            ?.filter { f ->
                val text = f.readText()
                // Match: non-data `class Foo ()` with no `val` properties
                text.contains(Regex("""\nclass \w+ \(\n\n\)""")) && !text.contains("val ")
            }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()

        if (emptyWrappers.isNotEmpty()) {
            // Delete the empty wrapper class files
            for (name in emptyWrappers) {
                val f = File(modelDir, "$name.kt")
                if (f.exists()) f.delete()
            }
            // Replace references in all generated files
            dir.get().asFile.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                var text = file.readText()
                var changed = false
                for (name in emptyWrappers) {
                    if (!text.contains(name)) continue
                    // Remove import lines FIRST (before replacing class names)
                    text = text.replace(Regex("""import [^\n]*\.$name\n"""), "")
                    // Replace type references in code
                    text = text.replace(Regex("""\b$name\b"""), "kotlinx.serialization.json.JsonElement")
                    changed = true
                }
                if (changed) file.writeText(text)
            }
        }

        dir.get().asFile.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            var text = file.readText()
            var changed = false

            // ── Fix 1: boolean const enums ──────────────────────────
            val enumDecl = Regex(
                """enum class (\w+)\(val value: kotlin\.Boolean\)"""
            )
            val names = enumDecl.findAll(text).map { it.groupValues[1] }.toList()
            for (name in names) {
                text = text.replace(Regex("""(val \w+:\s*)\w+\.$name""")) { m ->
                    "${m.groupValues[1]}kotlin.Boolean"
                }
                text = text.replace(Regex(
                    """\n\s*@Serializable\s*\n\s*enum class $name\(val value: kotlin\.Boolean\)\s*\{[^}]*\}"""
                ), "")
                text = text.replace(Regex(
                    """\n\s*/\*\*\s*\n(\s*\*[^\n]*\n)*\s*\*/\s*(?=\n\s*\n)"""
                ), "")
                changed = true
            }

            // ── Fix 2: double-parentheses `HashMap<...>()()` ────────
            if (text.contains("()()")) {
                text = text.replace("()()", "()")
                changed = true
            }

            // ── Fix 3: `kotlin.Double("...")` → double literal ──────
            val doubleCtorPattern = Regex("""kotlin\.Double\("(\d+(?:\.\d+)?)"\)""")
            if (doubleCtorPattern.containsMatchIn(text)) {
                text = doubleCtorPattern.replace(text) { m ->
                    val num = m.groupValues[1]
                    if (num.contains(".")) num else "$num.0"
                }
                changed = true
            }

            // ── Fix 4: add @Contextual to bare `kotlin.Any` usages ──
            // kotlinx.serialization cannot handle kotlin.Any without @Contextual.
            // Only patch @Serializable data class files (which import Contextual).
            // Skip enum files, API client files, and infrastructure.
            if (text.contains("kotlin.Any") &&
                text.contains("import kotlinx.serialization.Contextual") &&
                text.contains("@Serializable") &&
                text.contains("data class")
            ) {
                // Add @Contextual before kotlin.Any in val/field type positions
                // Covers: `val foo: kotlin.Any`, `Map<String, kotlin.Any>`, etc.
                text = text.replace(
                    Regex("""(?<!@Contextual )kotlin\.Any"""),
                    "@Contextual kotlin.Any"
                )
                changed = true
            }

            // ── Fix 5: nullable body in ApiClient ───────────────────
            // OkHttp's response.body is nullable but the generated code
            // dereferences it without null checks in two places:
            // a) responseBody() — add null guard so body smart-casts
            // b) request() error branches — `it.body.string()` → `it.body?.string()`
            if (file.name == "ApiClient.kt") {
                val guard = "val body = response.body"
                if (text.contains(guard) && !text.contains("if (body == null) return null")) {
                    text = text.replace(
                        guard,
                        "$guard\n        if (body == null) return null"
                    )
                    // After the null guard, remove safe calls that cause
                    // InputStream? issues (body is smart-cast non-null).
                    text = text.replace("body?.", "body.")
                    changed = true
                }
                // Fix `it.body.string()` in error branches of request()
                if (text.contains("it.body.string()")) {
                    text = text.replace("it.body.string()", "it.body?.string()")
                    changed = true
                }
            }

            // ── Fix 6: register AnySerializer in Serializer.kt ──────
            // kotlinx.serialization needs a contextual serializer for Any
            // that delegates to JsonElement for dynamic JSON values.
            if (file.name == "Serializer.kt") {
                if (!text.contains("AnySerializer")) {
                    // Add import + serializer object at end of file
                    text = text.replace(
                        "import kotlinx.serialization.modules.SerializersModuleBuilder",
                        "import kotlinx.serialization.modules.SerializersModuleBuilder\n" +
                        "import kotlinx.serialization.KSerializer\n" +
                        "import kotlinx.serialization.descriptors.SerialDescriptor\n" +
                        "import kotlinx.serialization.encoding.Decoder\n" +
                        "import kotlinx.serialization.encoding.Encoder\n" +
                        "import kotlinx.serialization.json.JsonDecoder\n" +
                        "import kotlinx.serialization.json.JsonEncoder\n" +
                        "import kotlinx.serialization.json.JsonElement"
                    )
                    // Register it in the SerializersModule
                    text = text.replace(
                        "contextual(StringBuilder::class, StringBuilderAdapter)",
                        "contextual(StringBuilder::class, StringBuilderAdapter)\n" +
                        "            contextual(Any::class, AnySerializer)"
                    )
                    // Append the serializer object before the closing of Serializer
                    text = text.trimEnd() + "\n\n" +
                        "internal object AnySerializer : KSerializer<Any> {\n" +
                        "    private val delegate = JsonElement.serializer()\n" +
                        "    override val descriptor: SerialDescriptor = delegate.descriptor\n" +
                        "    override fun serialize(encoder: Encoder, value: Any) {\n" +
                        "        val json = (encoder as JsonEncoder).json\n" +
                        "        encoder.encodeSerializableValue(delegate, json.encodeToJsonElement(delegate, value as? JsonElement ?: return))\n" +
                        "    }\n" +
                        "    override fun deserialize(decoder: Decoder): Any {\n" +
                        "        return (decoder as JsonDecoder).decodeJsonElement()\n" +
                        "    }\n" +
                        "}\n"
                    changed = true
                }
            }

            if (changed) file.writeText(text)
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn(fixGeneratedApi)
}

val cliDir = layout.buildDirectory.dir("generated/cli/cli")
val production = providers.gradleProperty("production").map { it.toBoolean() }.orElse(false)

val requiredPlatforms = listOf(
    "darwin-arm64",
    "darwin-x64",
    "linux-arm64",
    "linux-x64",
    "windows-x64",
    "windows-arm64",
)

val localCli by tasks.registering(PrepareLocalCliTask::class) {
    description = "Prepare local CLI binary for JetBrains dev"
    val os = providers.systemProperty("os.name").map {
        val name = it.lowercase()
        if (name.contains("mac")) return@map "darwin"
        if (name.contains("win")) return@map "windows"
        if (name.contains("linux")) return@map "linux"
        throw GradleException("Unsupported host OS: $it")
    }
    val arch = providers.systemProperty("os.arch").map {
        val name = it.lowercase()
        if (name == "aarch64" || name == "arm64") return@map "arm64"
        if (name == "x86_64" || name == "amd64") return@map "x64"
        throw GradleException("Unsupported host arch: $it")
    }
    script.set(rootProject.layout.projectDirectory.file("script/build.ts"))
    root.set(rootProject.layout.projectDirectory)
    out.set(cliDir)
    platform.set(os.zip(arch) { a, b -> "$a-$b" })
    exe.set(platform.map { if (it.startsWith("windows")) "kilo.exe" else "kilo" })
}

val checkCli by tasks.registering {
    description = "Verify CLI binaries exist before building"
    val dir = cliDir.map { it.asFile }
    val prod = production.get()
    val platforms = requiredPlatforms.toList()
    if (!prod) {
        dependsOn(localCli)
    }
    doLast {
        val resolved = dir.get()
        if (!resolved.exists() || resolved.listFiles()?.isEmpty() != false) {
            throw GradleException(
                "CLI binaries not found at ${resolved.absolutePath}.\n" +
                "Run 'bun run build' from packages/kilo-jetbrains/ to build CLI and plugin together."
            )
        }
        if (prod) {
            val missing = platforms.filter { platform ->
                val dir = File(resolved, platform)
                val exe = if (platform.startsWith("windows")) "kilo.exe" else "kilo"
                !File(dir, exe).exists()
            }
            if (missing.isNotEmpty()) {
                throw GradleException(
                    "Production build requires all platform CLI binaries.\n" +
                    "Missing: ${missing.joinToString(", ")}\n" +
                    "Run 'bun run build:production' to build all platforms."
                )
            }
        }
    }
}

tasks.processResources {
    dependsOn(checkCli)
}

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellij.platform)
        bundledModule("intellij.platform.kernel.backend")
        bundledModule("intellij.platform.rpc.backend")
        bundledModule("intellij.platform.backend")
    }

    implementation(project(":shared"))
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
}
