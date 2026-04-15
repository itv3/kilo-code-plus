package ai.kilocode.client

import ai.kilocode.client.app.KiloAppService
import ai.kilocode.client.app.KiloSessionService
import ai.kilocode.client.session.SessionUi
import ai.kilocode.client.app.KiloWorkspaceService
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
 * Creates a workspace for the project's base path and passes it to
 * [SessionUi]. Directory resolution (split-mode) happens lazily
 * inside the session when the status panel is shown.
 */
class KiloToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {
        private val LOG = Logger.getInstance(KiloToolWindowFactory::class.java)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        try {
            val workspaces = service<KiloWorkspaceService>()
            val sessions = project.service<KiloSessionService>()
            val app = service<KiloAppService>()
            val cs = CoroutineScope(SupervisorJob())

            val workspace = workspaces.workspace(project.basePath ?: "")
            val chat = SessionUi(project, workspace, sessions, app, cs)
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
