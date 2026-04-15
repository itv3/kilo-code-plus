@file:Suppress("UnstableApiUsage")

package ai.kilocode.client

import ai.kilocode.rpc.KiloSessionRpcApi
import ai.kilocode.rpc.dto.ChatEventDto
import ai.kilocode.rpc.dto.ConfigUpdateDto
import ai.kilocode.rpc.dto.MessageWithPartsDto
import ai.kilocode.rpc.dto.PromptDto
import ai.kilocode.rpc.dto.PromptPartDto
import ai.kilocode.rpc.dto.SessionDto
import ai.kilocode.rpc.dto.SessionStatusDto
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import fleet.rpc.client.durable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Project-level frontend service for session management and chat.
 *
 * Provides session CRUD, active session tracking, live status
 * updates, and chat operations via [KiloSessionRpcApi]. All
 * operations are scoped to the project's [directory] by default,
 * with support for per-session worktree directory overrides.
 */
@Service(Service.Level.PROJECT)
class KiloSessionService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    companion object {
        private val LOG = Logger.getInstance(KiloSessionService::class.java)
    }

    /**
     * The real project directory, resolved from [KiloProjectService].
     * Falls back to [Project.getBasePath] if not yet resolved.
     */
    private val directory: String
        get() {
            val resolved = project.service<KiloProjectService>().directory.value
            if (resolved.isNotEmpty()) return resolved
            val path = project.basePath ?: ""
            if (path.isEmpty()) {
                LOG.warn("project.basePath is null/empty — session operations will likely fail")
            }
            return path
        }

    private val _sessions = MutableStateFlow<List<SessionDto>>(emptyList())
    val sessions: StateFlow<List<SessionDto>> = _sessions.asStateFlow()

    private val _active = MutableStateFlow<SessionDto?>(null)
    val active: StateFlow<SessionDto?> = _active.asStateFlow()

    /** Live session status map from SSE events. */
    val statuses: StateFlow<Map<String, SessionStatusDto>> = flow {
        durable {
            KiloSessionRpcApi.getInstance()
                .statuses()
                .collect { emit(it) }
        }
    }.stateIn(cs, SharingStarted.Eagerly, emptyMap())

    /** Refresh the session list from the server. */
    fun refresh() {
        cs.launch {
            try {
                val result = durable { KiloSessionRpcApi.getInstance().list(directory) }
                _sessions.value = result.sessions
            } catch (e: Exception) {
                LOG.warn("session list failed", e)
            }
        }
    }

    /** Create a new session and make it active. */
    fun create() {
        cs.launch {
            try {
                val session = durable { KiloSessionRpcApi.getInstance().create(directory) }
                _active.value = session
                refresh()
            } catch (e: Exception) {
                LOG.warn("session create failed", e)
            }
        }
    }

    /** Select an existing session as active. */
    fun select(id: String) {
        cs.launch {
            try {
                val session = durable { KiloSessionRpcApi.getInstance().get(id, directory) }
                _active.value = session
            } catch (e: Exception) {
                LOG.warn("session select failed", e)
            }
        }
    }

    /** Delete a session. Clears active if it was the deleted one. */
    fun delete(id: String) {
        cs.launch {
            try {
                durable { KiloSessionRpcApi.getInstance().delete(id, directory) }
                if (_active.value?.id == id) _active.value = null
                refresh()
            } catch (e: Exception) {
                LOG.warn("session delete failed", e)
            }
        }
    }

    /** Register a worktree directory override for a session. */
    fun setDirectory(id: String, dir: String) {
        cs.launch {
            try {
                durable { KiloSessionRpcApi.getInstance().setDirectory(id, dir) }
            } catch (e: Exception) {
                LOG.warn("setDirectory failed", e)
            }
        }
    }

    // ------ chat ------

    /**
     * Send a text prompt to the active session. Creates a session if needed.
     *
     * @param text The user's message text
     * @param providerID Optional model override (provider part)
     * @param modelID Optional model override (model part)
     * @param agent Optional agent/mode override (e.g. "ask", "code")
     */
    fun prompt(text: String, providerID: String? = null, modelID: String? = null, agent: String? = null) {
        cs.launch {
            try {
                LOG.info("prompt: ensuring session exists (active=${_active.value?.id})")
                val session = ensureSession()
                LOG.info("prompt: session=${session.id}, dir=$directory, text=${text.take(80)}")
                val prompt = PromptDto(
                    parts = listOf(PromptPartDto(type = "text", text = text)),
                    providerID = providerID,
                    modelID = modelID,
                    agent = agent,
                )
                LOG.info("prompt: calling RPC prompt...")
                durable { KiloSessionRpcApi.getInstance().prompt(session.id, directory, prompt) }
                LOG.info("prompt: RPC returned successfully")
            } catch (e: Exception) {
                LOG.warn("prompt failed", e)
            }
        }
    }

    /** Abort ongoing processing for the active session. */
    fun abort() {
        cs.launch {
            val session = _active.value ?: return@launch
            try {
                durable { KiloSessionRpcApi.getInstance().abort(session.id, directory) }
            } catch (e: Exception) {
                LOG.warn("abort failed", e)
            }
        }
    }

    /** Load message history for the active session. */
    suspend fun messages(): List<MessageWithPartsDto> {
        val session = _active.value ?: return emptyList()
        return try {
            durable { KiloSessionRpcApi.getInstance().messages(session.id, directory) }
        } catch (e: Exception) {
            LOG.warn("messages failed", e)
            emptyList()
        }
    }

    /**
     * Subscribe to streaming chat events for the active session.
     * Returns an empty flow if no session is active.
     */
    fun events(): Flow<ChatEventDto> {
        val session = _active.value ?: return emptyFlow()
        return flow {
            durable {
                KiloSessionRpcApi.getInstance()
                    .events(session.id, directory)
                    .collect { emit(it) }
            }
        }
    }

    /** Update config (model, agent/mode, temperature). */
    fun updateConfig(config: ConfigUpdateDto) {
        cs.launch {
            try {
                durable { KiloSessionRpcApi.getInstance().updateConfig(directory, config) }
            } catch (e: Exception) {
                LOG.warn("config update failed", e)
            }
        }
    }

    // ------ helpers ------

    /**
     * Ensure an active session exists. Creates one if needed.
     */
    private suspend fun ensureSession(): SessionDto {
        _active.value?.let { return it }
        val dir = directory
        LOG.info("ensureSession: creating new session in dir=$dir")
        val session = durable { KiloSessionRpcApi.getInstance().create(dir) }
        LOG.info("ensureSession: created session ${session.id}")
        _active.value = session
        refresh()
        return session
    }
}
