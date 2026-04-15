package ai.kilocode.client.chat

import ai.kilocode.client.chat.model.SessionEvent
import ai.kilocode.client.chat.model.SessionModel
import ai.kilocode.client.chat.model.SessionModelListener
import com.intellij.openapi.Disposable

/**
 * View layer that subscribes to [SessionModel] events and manages
 * a [MessageListPanel].
 *
 * Implements [Disposable] — when disposed, the listener is
 * auto-removed via `Disposer` (registered in [SessionModel.addListener]).
 *
 * All callbacks run on the EDT (guaranteed by [SessionModel]).
 * Every event handler calls [refresh] to trigger `revalidate()`
 * and `repaint()` — no batching or optimization for now.
 */
class SessionUi(
    private val model: SessionModel,
) : SessionModelListener, Disposable {

    val panel = MessageListPanel()

    init {
        model.addListener(this, this)
    }

    override fun onEvent(event: SessionEvent) {
        when (event) {
            is SessionEvent.MessageAdded -> {
                val msg = model.chat.message(event.id) ?: return
                panel.addMessage(msg.info)
                refresh()
            }

            is SessionEvent.MessageRemoved -> {
                panel.removeMessage(event.id)
                refresh()
            }

            is SessionEvent.PartUpdated -> {
                val part = model.chat.part(event.messageId, event.partId) ?: return
                panel.updatePartText(event.messageId, event.partId, part.text.toString())
                refresh()
            }

            is SessionEvent.PartDelta -> {
                panel.appendDelta(event.messageId, event.partId, event.delta)
                refresh()
            }

            is SessionEvent.StatusChanged -> {
                panel.setStatus(event.text)
                refresh()
            }

            is SessionEvent.Error -> {
                panel.addError(event.message)
                refresh()
            }

            is SessionEvent.HistoryLoaded -> {
                panel.clear()
                for (msg in model.chat.messages()) {
                    panel.addMessage(msg.info)
                    for ((partId, part) in msg.parts) {
                        if (part.dto.type == "text" && part.text.isNotEmpty()) {
                            panel.updatePartText(msg.info.id, partId, part.text.toString())
                        }
                    }
                }
                refresh()
            }

            is SessionEvent.Cleared -> {
                panel.clear()
                refresh()
            }

            is SessionEvent.BusyChanged,
            is SessionEvent.WorkspaceReady,
            is SessionEvent.ViewChanged -> {
                // Handled by ChatPanel, not SessionUi
            }
        }
    }

    private fun refresh() {
        panel.revalidate()
        panel.repaint()
    }

    override fun dispose() {
        // Listener auto-removed by Disposer (registered in init via addListener)
    }
}
