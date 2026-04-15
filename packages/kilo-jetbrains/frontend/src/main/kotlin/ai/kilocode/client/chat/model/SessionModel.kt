package ai.kilocode.client.chat.model

import ai.kilocode.client.KiloProjectService
import ai.kilocode.client.KiloSessionService
import ai.kilocode.rpc.dto.ChatEventDto
import ai.kilocode.rpc.dto.ConfigUpdateDto
import ai.kilocode.rpc.dto.KiloWorkspaceStatusDto
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Session lifecycle controller that bridges coroutine flows to the EDT.
 *
 * Owns [ChatModel] and the listener list. All model mutations and
 * listener notifications happen on the EDT — callers (e.g. [SessionUi][ai.kilocode.client.chat.SessionUi])
 * can read [chat] directly without synchronization.
 *
 * **Thread model**: coroutines collect events from RPC flows on a
 * background thread, then `invokeLater` dispatches to EDT where the
 * model is updated and listeners are fired.
 */
class SessionModel(
    private val sessions: KiloSessionService,
    private val workspace: KiloProjectService,
    private val cs: CoroutineScope,
) : Disposable {

    val chat = ChatModel()

    private val listeners = mutableListOf<SessionModelListener>()

    // Status computation state (EDT-only)
    private var partType: String? = null
    private var tool: String? = null

    // Coroutine job for the current event subscription
    private var eventJob: Job? = null

    // --- Listener management (EDT) ---

    /**
     * Register a listener whose lifetime is tied to [parent].
     * When [parent] is disposed the listener is auto-removed.
     */
    fun addListener(parent: Disposable, listener: SessionModelListener) {
        listeners.add(listener)
        Disposer.register(parent) { listeners.remove(listener) }
    }

    // --- Actions (called from EDT) ---

    fun prompt(text: String) {
        showMessages()
        sessions.prompt(text)
    }

    fun abort() {
        sessions.abort()
    }

    fun selectAgent(name: String) {
        chat.agent = name
        sessions.updateConfig(ConfigUpdateDto(agent = name))
        fire(SessionEvent.WorkspaceReady)
    }

    fun selectModel(provider: String, id: String) {
        chat.model = "$provider/$id"
        sessions.updateConfig(ConfigUpdateDto(model = "$provider/$id"))
        fire(SessionEvent.WorkspaceReady)
    }

    // --- Internal: coroutine → EDT bridge ---

    init {
        // Watch active session changes
        cs.launch {
            sessions.active.collect { session ->
                edt {
                    chat.clear()
                    partType = null
                    tool = null
                    hideMessages()
                    fire(SessionEvent.Cleared)
                }
                eventJob?.cancel()
                if (session != null) {
                    loadHistory()
                    subscribeEvents()
                }
            }
        }

        // Watch session statuses for busy/idle
        cs.launch {
            sessions.statuses.collect { statuses ->
                val active = sessions.active.value?.id ?: return@collect
                val st = statuses[active]
                edt { fire(SessionEvent.BusyChanged(st?.type == "busy")) }
            }
        }

        // Watch workspace state for providers/agents
        cs.launch {
            workspace.state.collect { state ->
                if (state.status == KiloWorkspaceStatusDto.READY) {
                    edt {
                        chat.agents = state.agents?.agents?.map {
                            AgentItem(it.name, it.displayName ?: it.name)
                        } ?: emptyList()

                        chat.models = state.providers?.let { providers ->
                            providers.providers
                                .filter { it.id in providers.connected }
                                .flatMap { provider ->
                                    provider.models.map { (id, info) ->
                                        ModelItem(id, info.name, provider.id)
                                    }
                                }
                        } ?: emptyList()

                        if (chat.agent == null) {
                            chat.agent = state.agents?.default
                        }
                        if (chat.model == null) {
                            chat.model = state.providers?.defaults?.entries?.firstOrNull()?.value
                        }

                        chat.ready = true
                        fire(SessionEvent.WorkspaceReady)
                    }
                }
            }
        }
    }

    private fun loadHistory() {
        cs.launch {
            val history = sessions.messages()
            edt {
                chat.load(history)
                if (!chat.isEmpty()) showMessages()
                fire(SessionEvent.HistoryLoaded)
            }
        }
    }

    private fun subscribeEvents() {
        eventJob = cs.launch {
            sessions.events().collect { event ->
                edt { handle(event) }
            }
        }
    }

    private fun handle(event: ChatEventDto) {
        when (event) {
            is ChatEventDto.MessageUpdated -> {
                chat.addMessage(event.info)
                showMessages()
                fire(SessionEvent.MessageAdded(event.info.id))
            }

            is ChatEventDto.PartUpdated -> {
                partType = event.part.type
                tool = event.part.tool
                chat.updatePart(event.part.messageID, event.part)
                fire(SessionEvent.StatusChanged(status()))
                if (event.part.type == "text" && event.part.text != null) {
                    fire(SessionEvent.PartUpdated(event.part.messageID, event.part.id))
                }
            }

            is ChatEventDto.PartDelta -> {
                if (event.field == "text") {
                    chat.appendDelta(event.messageID, event.partID, event.delta)
                    fire(SessionEvent.PartDelta(event.messageID, event.partID, event.delta))
                }
            }

            is ChatEventDto.TurnOpen -> {
                partType = null
                tool = null
                fire(SessionEvent.StatusChanged("Considering next steps..."))
                fire(SessionEvent.BusyChanged(true))
            }

            is ChatEventDto.TurnClose -> {
                partType = null
                tool = null
                fire(SessionEvent.StatusChanged(null))
                fire(SessionEvent.BusyChanged(false))
            }

            is ChatEventDto.Error -> {
                val msg = event.error?.message ?: event.error?.type ?: "Unknown error"
                fire(SessionEvent.Error(msg))
                fire(SessionEvent.StatusChanged(null))
                fire(SessionEvent.BusyChanged(false))
            }

            is ChatEventDto.MessageRemoved -> {
                chat.removeMessage(event.messageID)
                fire(SessionEvent.MessageRemoved(event.messageID))
            }
        }
    }

    // --- View switching (EDT) ---

    private fun showMessages() {
        if (!chat.showMessages) {
            chat.showMessages = true
            fire(SessionEvent.ViewChanged(true))
        }
    }

    private fun hideMessages() {
        if (chat.showMessages) {
            chat.showMessages = false
            fire(SessionEvent.ViewChanged(false))
        }
    }

    /**
     * Compute a human-readable status from the last streaming part.
     * Mirrors the VS Code extension's `computeStatus()` logic.
     */
    private fun status(): String = when (partType) {
        "reasoning" -> "Thinking..."
        "text" -> "Writing response..."
        "tool" -> when (tool) {
            "task" -> "Delegating work..."
            "todowrite", "todoread" -> "Planning..."
            "read" -> "Gathering context..."
            "glob", "grep", "list" -> "Searching codebase..."
            "webfetch", "websearch", "codesearch" -> "Searching web..."
            "edit", "write" -> "Making edits..."
            "bash" -> "Running commands..."
            else -> "Considering next steps..."
        }
        else -> "Considering next steps..."
    }

    private fun fire(event: SessionEvent) {
        for (l in listeners) l.onEvent(event)
    }

    private fun edt(block: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(block)
    }

    override fun dispose() {
        eventJob?.cancel()
        cs.cancel()
    }
}
