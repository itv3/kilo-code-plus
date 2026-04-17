package ai.kilocode.client.session.model.message

/** Typed content within a message. */
sealed class Content(val id: String)

/** Streamed text content from the assistant. */
class Text(id: String) : Content(id) {
    val content = StringBuilder()
}

/** Model reasoning / chain-of-thought. */
class Reasoning(id: String) : Content(id) {
    val content = StringBuilder()
}

/** Tool invocation with lifecycle state. */
class Tool(id: String, val name: String) : Content(id) {
    var state: ToolExecState = ToolExecState.PENDING
    var title: String? = null
}

/** Context compaction marker. */
class Compaction(id: String) : Content(id)

enum class ToolExecState { PENDING, RUNNING, COMPLETED, ERROR }
