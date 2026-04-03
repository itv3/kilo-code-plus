package ai.kilocode.jetbrains

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel

class KiloToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, window: ToolWindow) {
        val panel = JPanel().apply {
            isFocusable = true
            focusTraversalKeysEnabled = false
        }
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.preferredFocusableComponent = panel
        window.contentManager.addContent(content)
    }
}
