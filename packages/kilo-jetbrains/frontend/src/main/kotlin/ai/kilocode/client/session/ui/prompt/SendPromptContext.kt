package ai.kilocode.client.session.ui.prompt

interface SendPromptContext {
    val isSendEnabled: Boolean

    fun send()
}
