package ai.kilocode.client

import ai.kilocode.client.chat.SessionUi
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Creates the Kilo Code tool window with a single [SessionUi].
 *
 * The chat panel shows a welcome/status view in the center until the
 * first prompt is sent, then switches to a scrollable message list.
 * No tabs — the chat panel is the only content.
 */
class KiloToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        private val LOG = Logger.getInstance(KiloToolWindowFactory::class.java)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        try {
            val app = service<KiloAppService>()
            val workspace = project.service<KiloProjectService>()
            val sessions = project.service<KiloSessionService>()
            val scope = CoroutineScope(SupervisorJob())

            val chat = SessionUi(project, app, workspace, sessions, scope)
            val content = ContentFactory.getInstance()
                .createContent(chat, "", false)
            content.setDisposer(chat)
            toolWindow.contentManager.addContent(content)

            ActionManager.getInstance().getAction("Kilo.Settings")?.let {
                toolWindow.setTitleActions(listOf(it))
            }
        } catch (e: Exception) {
            LOG.error("Failed to create Kilo tool window content", e)
        }
    }
}
