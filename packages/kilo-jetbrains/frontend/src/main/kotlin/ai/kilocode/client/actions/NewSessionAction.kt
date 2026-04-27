package ai.kilocode.client.actions

import ai.kilocode.client.plugin.KiloBundle
import ai.kilocode.client.session.SessionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.IconLoader

class NewSessionAction : AnAction(
    KiloBundle.message("action.Kilo.NewSession.text"),
    KiloBundle.message("action.Kilo.NewSession.description"),
    IconLoader.getIcon("/icons/plus.svg", NewSessionAction::class.java),
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        e.getData(SessionManager.KEY)?.newSession()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(SessionManager.KEY) != null
    }
}
