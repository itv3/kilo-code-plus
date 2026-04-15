package ai.kilocode.client.chat

import ai.kilocode.client.KiloAppService
import ai.kilocode.client.KiloProjectService
import ai.kilocode.client.KiloSessionService
import ai.kilocode.client.chat.model.SessionEvent
import ai.kilocode.client.chat.model.SessionModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JPanel

/**
 * Main chat panel — pure Swing layout that reacts to [SessionModel] events.
 *
 * Uses [CardLayout] in the center to switch between the empty panel
 * (shown before the first prompt) and the scrollable message list.
 *
 * All business logic (workspace watching, session lifecycle, event
 * handling, status computation) lives in [SessionModel]. Message
 * rendering lives in [SessionUi]. This class only wires layout,
 * prompt callbacks, and reacts to model events for card switching,
 * picker population, busy state, and scrolling.
 */
class ChatPanel(
    project: Project,
    app: KiloAppService,
    workspace: KiloProjectService,
    sessions: KiloSessionService,
    cs: CoroutineScope,
) : JPanel(BorderLayout()), Disposable {

    companion object {
        private const val WELCOME = "welcome"
        private const val MESSAGES = "messages"
    }

    private val model = SessionModel(sessions, workspace, cs)
    private val session = SessionUi(model)

    private val cards = CardLayout()
    private val center = JPanel(cards)

    private val welcome = EmptyChatUi(app, workspace, cs)

    private val scroll = JBScrollPane(session.panel).apply {
        border = JBUI.Borders.empty()
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    private val prompt = PromptPanel(
        project = project,
        onSend = { text -> send(text) },
        onAbort = { model.abort() },
    )

    init {
        Disposer.register(this, session)
        Disposer.register(this, model)

        // Layout
        center.add(welcome, WELCOME)
        center.add(scroll, MESSAGES)
        cards.show(center, WELCOME)

        add(center, BorderLayout.CENTER)
        add(prompt, BorderLayout.SOUTH)

        // Wire picker callbacks via typed model methods
        prompt.mode.onSelect = { item ->
            model.selectAgent(item.id)
        }
        prompt.model.onSelect = { item ->
            val group = item.group
            if (group != null) {
                model.selectModel(group, item.id)
            }
        }

        // React to model events — no coroutines, pure EDT
        model.addListener(this) { event ->
            when (event) {
                is SessionEvent.WorkspaceReady -> {
                    val c = model.chat
                    prompt.mode.setItems(
                        c.agents.map { LabelPicker.Item(it.name, it.display) },
                        c.agent,
                    )
                    prompt.model.setItems(
                        c.models.map { LabelPicker.Item(it.id, it.display, it.provider) },
                        c.model,
                    )
                    prompt.setReady(c.ready)
                }

                is SessionEvent.ViewChanged -> {
                    cards.show(center, if (event.show) MESSAGES else WELCOME)
                }

                is SessionEvent.BusyChanged -> {
                    prompt.setBusy(event.busy)
                }

                is SessionEvent.MessageAdded,
                is SessionEvent.PartUpdated,
                is SessionEvent.PartDelta,
                is SessionEvent.Error,
                is SessionEvent.HistoryLoaded -> {
                    scrollToBottom()
                }

                else -> {}
            }
        }
    }

    private fun send(text: String) {
        if (text.isBlank()) return
        model.prompt(text)
        prompt.clear()
    }

    private fun scrollToBottom() {
        val bar = scroll.verticalScrollBar
        bar.value = bar.maximum
    }

    override fun dispose() {
        welcome.dispose()
        // session and model disposed by Disposer (registered as children)
    }
}
