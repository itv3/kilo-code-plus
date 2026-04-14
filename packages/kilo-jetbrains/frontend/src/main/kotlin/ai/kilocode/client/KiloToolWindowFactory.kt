package ai.kilocode.client

import ai.kilocode.client.chat.ChatPanel
import ai.kilocode.rpc.dto.KiloAppStatusDto
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Creates the Kilo Code tool window content.
 *
 * Starts with a [KiloWelcomeUi] status panel. Once the backend reaches
 * [KiloAppStatusDto.READY], adds a [ChatPanel] tab and switches to it.
 */
class KiloToolWindowFactory : ToolWindowFactory {

    companion object {
        private val LOG = Logger.getInstance(KiloToolWindowFactory::class.java)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        try {
            val app = service<KiloAppService>()
            val workspace = project.service<KiloProjectService>()
            val sessions = project.service<KiloSessionService>()
            val scope = CoroutineScope(SupervisorJob())

            // Welcome/status tab
            val welcome = KiloWelcomeUi(app, workspace, scope)
            val statusContent = ContentFactory.getInstance()
                .createContent(welcome, "Status", false)
            statusContent.setDisposer(welcome)
            toolWindow.contentManager.addContent(statusContent)

            // Chat tab — added once the backend is ready
            val chatScope = CoroutineScope(SupervisorJob())
            val chat = ChatPanel(sessions, workspace, chatScope)
            val chatContent = ContentFactory.getInstance()
                .createContent(chat, "Chat", false)
            chatContent.setDisposer(chat)
            toolWindow.contentManager.addContent(chatContent)

            // Switch to chat tab when ready
            scope.launch {
                app.state.collect { state ->
                    if (state.status == KiloAppStatusDto.READY) {
                        toolWindow.contentManager.setSelectedContent(chatContent)
                    }
                }
            }

            ActionManager.getInstance().getAction("Kilo.Settings")?.let {
                toolWindow.setTitleActions(listOf(it))
            }
        } catch (e: Exception) {
            LOG.error("Failed to create Kilo tool window content", e)
        }
    }
}
