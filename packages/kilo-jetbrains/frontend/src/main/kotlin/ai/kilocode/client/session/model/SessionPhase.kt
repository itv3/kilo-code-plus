package ai.kilocode.client.session.model

/**
 * Single source of truth for what a session is doing right now.
 *
 * Variants are mutually exclusive — the UI switches on a single value.
 */
sealed class SessionPhase {
    /** Nothing happening. Ready for user input. */
    data object Idle : SessionPhase()

    /** LLM is actively processing. */
    data class Working(val status: StatusState) : SessionPhase()

    /** Waiting for user to answer a question or approve a permission. */
    data class Prompting(val prompt: PromptState) : SessionPhase()

    /** Request failed, retrying with backoff. */
    data class Retry(val attempt: Int, val message: String, val next: Long) : SessionPhase()

    /** Cannot reach the provider. */
    data class Offline(val requestId: String, val message: String) : SessionPhase()

    /** Turn ended with an error. User can still send a new prompt. */
    data class Error(val message: String, val kind: String? = null) : SessionPhase()
}
