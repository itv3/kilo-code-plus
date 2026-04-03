package ai.kilocode.jetbrains

import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

class KiloToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel().apply {
            layout = GridBagLayout()
            isFocusable = true
            focusTraversalKeysEnabled = false
        }

        val icon = JBLabel(
            IconUtil.scale(IconLoader.getIcon("/icons/kilo-content.svg", KiloToolWindowFactory::class.java), null, 0.75f),
        ).apply {
            horizontalAlignment = SwingConstants.CENTER
            alignmentX = JPanel.CENTER_ALIGNMENT
        }

        val text = JBLabel(
            "<html><div style='text-align:center; width:${JBUI.scale(260)}px;'>" +
                "Kilo Code is an AI coding assistant. Ask it to build features, fix bugs, or explain your codebase." +
                "</div></html>",
            SwingConstants.CENTER,
        ).apply {
            alignmentX = JPanel.CENTER_ALIGNMENT
            font = JBUI.Fonts.label(13f)
            foreground = UIUtil.getContextHelpForeground()
        }

        val body = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(icon)
            add(Box.createVerticalStrut(JBUI.scale(24)))
            add(text)
        }

        panel.add(body)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.preferredFocusableComponent = panel
        toolWindow.contentManager.addContent(content)
    }
}
