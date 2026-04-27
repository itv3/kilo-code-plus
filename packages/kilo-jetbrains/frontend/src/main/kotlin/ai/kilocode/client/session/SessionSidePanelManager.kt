package ai.kilocode.client.session

import ai.kilocode.client.app.KiloWorkspaceService
import ai.kilocode.client.app.Workspace
import ai.kilocode.rpc.dto.SessionDto
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.BorderLayout
import javax.swing.JPanel

class SessionSidePanelManager(
    private val project: Project,
    private val root: Workspace,
    private val create: (Project, Workspace, SessionManager, String?) -> SessionUi = { project, workspace, manager, id ->
        service<SessionUiFactory>().create(project, workspace, manager, id)
    },
    private val resolve: (String) -> Workspace = { dir -> service<KiloWorkspaceService>().workspace(dir) },
) : SessionManager, Disposable {
    val component: JPanel = object : JPanel(BorderLayout()), DataProvider {
        override fun getData(dataId: String): Any? {
            if (SessionManager.KEY.`is`(dataId)) return this@SessionSidePanelManager
            return null
        }
    }

    private val opened = mutableMapOf<String, SessionUi>()
    private val all = mutableSetOf<SessionUi>()
    private var current: SessionUi? = null

    override fun newSession() {
        show(create(project, root, this, null))
    }

    override fun openSession(session: SessionDto) {
        val ui = opened.getOrPut(session.id) {
            create(project, resolve(session.directory), this, session.id).also {
                all.add(it)
            }
        }
        show(ui)
    }

    private fun show(ui: SessionUi) {
        all.add(ui)
        if (current === ui) return
        component.removeAll()
        current = ui
        component.add(ui, BorderLayout.CENTER)
        component.revalidate()
        component.repaint()
    }

    override fun dispose() {
        val items = all.toList()
        opened.clear()
        all.clear()
        current = null
        component.removeAll()
        items.forEach { Disposer.dispose(it) }
    }
}
