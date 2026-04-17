package ai.kilocode.client.session.model

/** Describes the LLM activity when `SessionPhase.Working`. */
sealed class StatusState {
    data class Thinking(val text: String) : StatusState()
    data class Working(val text: String) : StatusState()
}
