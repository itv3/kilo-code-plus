package ai.kilocode.backend

import ai.kilocode.jetbrains.api.client.DefaultApi
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Project-level backend service that delegates to the app-level
 * [KiloBackendAppService] and scopes CLI API calls to this project's
 * working directory.
 *
 * The VS Code extension likewise scopes calls via `x-kilo-directory`.
 * In the JetBrains plugin this is achieved by passing the directory
 * parameter to each generated API method.
 *
 * Currently a thin shell — will hold project-scoped data loading
 * (providers, agents, config, etc.) in the future.
 */
@Service(Service.Level.PROJECT)
class KiloBackendProjectService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    companion object {
        private val LOG = Logger.getInstance(KiloBackendProjectService::class.java)
    }

    private val app: KiloBackendAppService
        get() = service()

    /** Project working directory sent as the `directory` parameter. */
    val directory: String
        get() = project.basePath ?: ""

    /** Connection state (delegates to app-level service). */
    val state: StateFlow<ConnectionState>
        get() = app.state

    /** Ensure the CLI backend is running and connected. */
    suspend fun connect() = app.connect()

    /** Kill the CLI process and restart it. */
    suspend fun restart() = app.restart()

    /** Kill the CLI process, re-extract the binary, and restart. */
    suspend fun reinstall() = app.reinstall()

    /**
     * The generated API client, or null when disconnected.
     *
     * Callers should pass [directory] to each API method's `directory`
     * parameter to scope requests to this project.
     */
    val api: DefaultApi?
        get() = app.api
}
