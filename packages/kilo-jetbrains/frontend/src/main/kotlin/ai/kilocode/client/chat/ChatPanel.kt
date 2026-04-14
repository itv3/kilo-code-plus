package ai.kilocode.client.chat

import ai.kilocode.client.KiloProjectService
import ai.kilocode.client.KiloSessionService
import ai.kilocode.rpc.dto.AgentsDto
import ai.kilocode.rpc.dto.ChatEventDto
import ai.kilocode.rpc.dto.ConfigUpdateDto
import ai.kilocode.rpc.dto.KiloWorkspaceStatusDto
import ai.kilocode.rpc.dto.MessageWithPartsDto
import ai.kilocode.rpc.dto.ModelDto
import ai.kilocode.rpc.dto.ProviderDto
import ai.kilocode.rpc.dto.ProvidersDto
import ai.kilocode.rpc.dto.SessionStatusDto
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Main chat panel composing the toolbar, message list, and input area.
 *
 * Wires [KiloSessionService] for chat operations and [KiloProjectService]
 * for provider/agent data. Subscribes to SSE chat events for streaming.
 */
class ChatPanel(
    private val sessions: KiloSessionService,
    private val workspace: KiloProjectService,
    private val cs: CoroutineScope,
) : JPanel(BorderLayout()), Disposable {

    private val messages = MessageListPanel()
    private val scroll = JBScrollPane(messages).apply {
        border = JBUI.Borders.empty()
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    private val toolbar = ChatToolbar(
        onModeChanged = { agent -> sessions.updateConfig(ConfigUpdateDto(agent = agent)) },
        onModelChanged = { provider, model -> sessions.updateConfig(ConfigUpdateDto(model = "$provider/$model")) },
    )

    private val input = ChatInputPanel(
        onSend = { text -> send(text) },
        onAbort = { sessions.abort() },
    )

    private var eventJob: Job? = null
    private var statusJob: Job? = null
    private var wsJob: Job? = null

    init {
        add(toolbar, BorderLayout.NORTH)
        add(scroll, BorderLayout.CENTER)
        add(input, BorderLayout.SOUTH)

        // Watch workspace state for providers/agents
        wsJob = cs.launch {
            workspace.state.collect { state ->
                if (state.status == KiloWorkspaceStatusDto.READY) {
                    edt {
                        state.providers?.let { toolbar.setProviders(it) }
                        state.agents?.let { toolbar.setAgents(it) }
                    }
                }
            }
        }

        // Watch session statuses for busy/idle state
        statusJob = cs.launch {
            sessions.statuses.collect { statuses ->
                val active = sessions.active.value?.id ?: return@collect
                val status = statuses[active]
                edt { input.setBusy(status?.type == "busy") }
            }
        }

        // Watch active session changes
        cs.launch {
            sessions.active.collect { session ->
                edt {
                    messages.clear()
                    input.setBusy(false)
                }
                eventJob?.cancel()
                if (session != null) {
                    loadHistory(session.id)
                    subscribeEvents()
                }
            }
        }
    }

    private fun send(text: String) {
        if (text.isBlank()) return
        sessions.prompt(text)
        input.clearInput()
    }

    private fun loadHistory(id: String) {
        cs.launch {
            val history = sessions.messages()
            edt {
                messages.clear()
                for (msg in history) {
                    messages.addMessage(msg.info)
                    for (part in msg.parts) {
                        val txt = part.text
                        if (part.type == "text" && txt != null) {
                            messages.updatePartText(msg.info.id, part.id, txt)
                        }
                    }
                }
                scrollToBottom()
            }
        }
    }

    private fun subscribeEvents() {
        eventJob = cs.launch {
            sessions.events().collect { event ->
                edt { handleEvent(event) }
            }
        }
    }

    private fun handleEvent(event: ChatEventDto) {
        when (event) {
            is ChatEventDto.MessageUpdated -> {
                messages.addMessage(event.info)
                scrollToBottom()
            }

            is ChatEventDto.PartUpdated -> {
                val txt = event.part.text
                if (event.part.type == "text" && txt != null) {
                    messages.updatePartText(event.part.messageID, event.part.id, txt)
                    scrollToBottom()
                }
            }

            is ChatEventDto.PartDelta -> {
                if (event.field == "text") {
                    messages.appendDelta(event.messageID, event.partID, event.delta)
                    scrollToBottom()
                }
            }

            is ChatEventDto.TurnOpen -> {
                input.setBusy(true)
            }

            is ChatEventDto.TurnClose -> {
                input.setBusy(false)
            }

            is ChatEventDto.Error -> {
                val msg = event.error?.message ?: event.error?.type ?: "Unknown error"
                messages.addError(msg)
                input.setBusy(false)
                scrollToBottom()
            }

            is ChatEventDto.MessageRemoved -> {
                messages.removeMessage(event.messageID)
            }
        }
    }

    private fun scrollToBottom() {
        val bar = scroll.verticalScrollBar
        bar.value = bar.maximum
    }

    private fun edt(block: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(block)
    }

    override fun dispose() {
        eventJob?.cancel()
        statusJob?.cancel()
        wsJob?.cancel()
        cs.cancel()
    }
}
