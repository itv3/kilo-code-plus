package ai.kilocode.client.session.model

/**
 * Lifecycle events fired by [SessionManager] on the EDT.
 *
 * These cover app/workspace state changes and view switching — things
 * outside the [SessionModel] domain. For model mutations (messages,
 * parts, phase), listen to [SessionModelEvent] on [SessionModel] directly.
 */
sealed class SessionManagerEvent {

    // App + workspace lifecycle (every state transition)
    data object AppChanged : SessionManagerEvent()
    data object WorkspaceChanged : SessionManagerEvent()

    // Workspace ready (pickers populated)
    data object WorkspaceReady : SessionManagerEvent()
    data class ViewChanged(val show: Boolean) : SessionManagerEvent()
}

/**
 * Listener for [SessionManagerEvent]s fired by [SessionManager].
 * All callbacks are guaranteed to run on the EDT.
 */
fun interface SessionManagerListener {
    fun onEvent(event: SessionManagerEvent)
}
