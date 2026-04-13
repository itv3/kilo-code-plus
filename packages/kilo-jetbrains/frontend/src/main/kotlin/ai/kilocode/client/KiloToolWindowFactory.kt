package ai.kilocode.client

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.CoroutineScope

/**
 * Creates the Kilo Code tool window content.
 *
 * Wires [KiloAppService] and [KiloProjectService] into a
 * [KiloWelcomeUi] panel that shows app + workspace initialization
 * status. All UI logic lives in [KiloWelcomeUi].
 */
class KiloToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val app = service<KiloAppService>()
        val workspace = project.service<KiloProjectService>()
        val scope = project.service<CoroutineScope>()
        val ui = KiloWelcomeUi(app, workspace, scope)

        val content = ContentFactory.getInstance().createContent(ui, "", false)
        content.setDisposer(ui)
        toolWindow.contentManager.addContent(content)

        ActionManager.getInstance().getAction("Kilo.Settings")?.let {
            toolWindow.setTitleActions(listOf(it))
        }
    }
}
