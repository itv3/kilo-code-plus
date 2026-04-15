package ai.kilocode.client.chat.model

/**
 * Change events fired by [SessionModel] on the EDT.
 *
 * Events carry IDs so the UI knows **which** message/part changed.
 * The UI can read full data from [ChatModel] directly (safe — same
 * EDT thread). [PartDelta] also carries the delta string so the
 * view can append efficiently without reading the whole text.
 */
sealed class SessionEvent {

    // Message lifecycle
    data class MessageAdded(val id: String) : SessionEvent()
    data class MessageRemoved(val id: String) : SessionEvent()

    // Part changes
    data class PartUpdated(val messageId: String, val partId: String) : SessionEvent()
    data class PartDelta(val messageId: String, val partId: String, val delta: String) : SessionEvent()

    // Session state
    data class StatusChanged(val text: String?) : SessionEvent()
    data class BusyChanged(val busy: Boolean) : SessionEvent()
    data class Error(val message: String) : SessionEvent()

    // Bulk operations
    data object HistoryLoaded : SessionEvent()
    data object Cleared : SessionEvent()

    // Workspace state
    data object WorkspaceReady : SessionEvent()
    data class ViewChanged(val show: Boolean) : SessionEvent()
}

/**
 * Listener for [SessionEvent]s fired by [SessionModel].
 * All callbacks are guaranteed to run on the EDT.
 */
fun interface SessionModelListener {
    fun onEvent(event: SessionEvent)
}
