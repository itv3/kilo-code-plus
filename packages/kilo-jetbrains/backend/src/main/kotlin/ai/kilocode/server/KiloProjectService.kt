package ai.kilocode.server

import ai.kilocode.jetbrains.api.client.DefaultApi
import ai.kilocode.rpc.dto.ConnectionStateDto
import ai.kilocode.rpc.dto.HealthDto
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Project-level backend service that delegates to the app-level
 * [KiloConnectionService] and scopes CLI API calls to this project's
 * working directory.
 *
 * The VS Code extension likewise scopes calls via `x-kilo-directory`.
 * In the JetBrains plugin this is achieved by passing the directory
 * parameter to each generated API method.
 */
@Service(Service.Level.PROJECT)
class KiloProjectService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    private val connection: KiloConnectionService
        get() = service()

    /** Project working directory sent as the `directory` parameter. */
    val directory: String
        get() = project.basePath ?: ""

    /** Connection state (delegates to app-level service). */
    val state: StateFlow<ConnectionState>
        get() = connection.state

    /** Connection state mapped to DTO for RPC transport. */
    fun stream() = connection.stream()

    /** Ensure the CLI backend is running and connected. */
    suspend fun connect() = connection.connect()

    /**
     * The generated API client, or null when disconnected.
     *
     * Callers should pass [directory] to each API method's `directory`
     * parameter to scope requests to this project.
     */
    val api: DefaultApi?
        get() = connection.api

    /**
     * One-shot health check via the generated API client.
     * Returns [HealthDto] or throws if not connected / server unreachable.
     */
    suspend fun health(): HealthDto {
        val client = api ?: throw IllegalStateException("Not connected")
        val response = client.globalHealth()
        return HealthDto(healthy = true, version = response.version)
    }
}
