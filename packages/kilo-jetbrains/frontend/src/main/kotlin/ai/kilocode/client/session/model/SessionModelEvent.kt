package ai.kilocode.client.session.model

/**
 * Change events fired by [SessionModel].
 *
 * Events carry the data needed for rendering so UI can update without
 * reading back from the model except for [HistoryLoaded].
 */
sealed class SessionModelEvent {
    data class MessageAdded(val info: Message) : SessionModelEvent() {
        override fun toString() = "MessageAdded ${info.info.id}"
    }
    data class MessageRemoved(val id: String) : SessionModelEvent() {
        override fun toString() = "MessageRemoved $id"
    }
    data class ContentAdded(val messageId: String, val content: Content) : SessionModelEvent() {
        override fun toString() = "ContentAdded $messageId/${content.id}"
    }
    data class ContentUpdated(val messageId: String, val content: Content) : SessionModelEvent() {
        override fun toString() = "ContentUpdated $messageId/${content.id}"
    }
    data class ContentDelta(val messageId: String, val contentId: String, val delta: String) : SessionModelEvent() {
        override fun toString() = "ContentDelta $messageId/$contentId"
    }
    data class StateChanged(val state: SessionState) : SessionModelEvent() {
        override fun toString() = "StateChanged ${state::class.simpleName}"
    }
    data object HistoryLoaded : SessionModelEvent()
    data object Cleared : SessionModelEvent()

    fun interface Listener {
        fun onEvent(event: SessionModelEvent)
    }
}
