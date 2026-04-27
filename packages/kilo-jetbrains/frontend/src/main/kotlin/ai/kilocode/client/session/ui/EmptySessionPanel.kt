package ai.kilocode.client.session.ui

import ai.kilocode.client.plugin.KiloBundle
import ai.kilocode.client.session.update.SessionController
import ai.kilocode.client.session.update.SessionControllerEvent
import ai.kilocode.client.session.update.SessionControllerListener
import ai.kilocode.client.ui.md.MdView
import ai.kilocode.rpc.dto.SessionDto
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.Centerizer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * Centered empty-session panel.
 */
class EmptySessionPanel(
    parent: Disposable,
    private val controller: SessionController,
    open: (SessionDto) -> Unit = {},
) : JPanel(BorderLayout()), SessionControllerListener, Disposable {

    companion object {
        internal const val LIMIT = 5
        internal const val MAX_WIDTH = 350
    }

    init {
        Disposer.register(parent, this)
        controller.addListener(this, this)
    }

    private val list = SessionListPanel(LIMIT, open)
    private val md = MdView.html().apply {
        opaque = false
        foreground = UIUtil.getContextHelpForeground()
        set(KiloBundle.message("session.empty.welcome"))
    }

    private val logo = JBLabel(
        IconLoader.getIcon("/icons/kilo-content.svg", EmptySessionPanel::class.java),
    ).apply {
        alignmentX = CENTER_ALIGNMENT
    }

    private val intro = JPanel(BorderLayout()).apply {
        isOpaque = false
        alignmentX = CENTER_ALIGNMENT
        add(md.component, BorderLayout.CENTER)
        border = JBUI.Borders.empty(0, 12, 0, 12)
    }

    private val recent = JPanel(BorderLayout()).apply {
        isOpaque = false
        alignmentX = CENTER_ALIGNMENT
        isVisible = false
        add(JBLabel(KiloBundle.message("session.empty.recent")).apply {
            foreground = UIUtil.getContextHelpForeground()
            font = font.deriveFont(font.size2D - 1f)
            border = JBUI.Borders.emptyLeft(8)
        }, BorderLayout.NORTH)
        add(list, BorderLayout.CENTER)
    }

    private val stack = JPanel().apply {
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(logo)
        add(Box.createVerticalStrut(JBUI.scale(14)))
        add(intro)
        add(Box.createVerticalStrut(JBUI.scale(28)))
        add(recent)
    }

    private val content = object : JPanel(BorderLayout()) {
        override fun getPreferredSize(): Dimension {
            val size = super.getPreferredSize()
            return Dimension(JBUI.scale(MAX_WIDTH), size.height)
        }
    }.apply {
        isOpaque = false
        add(stack, BorderLayout.NORTH)
    }

    private var loading = false

    init {
        isOpaque = false
        border = JBUI.Borders.empty(12)
        add(Centerizer(content, Centerizer.TYPE.BOTH), BorderLayout.CENTER)
        refresh()
    }

    override fun addNotify() {
        super.addNotify()
        refresh(force = true)
    }

    override fun onEvent(event: SessionControllerEvent) {
        when (event) {
            is SessionControllerEvent.AppChanged,
            is SessionControllerEvent.WorkspaceChanged,
            is SessionControllerEvent.WorkspaceReady -> {
                if (controller.ready) refresh()
            }

            is SessionControllerEvent.ViewChanged -> {
                if (!event.show) refresh(force = true)
            }
        }
    }

    fun refresh(force: Boolean = false) {
        if (!force && loading) return
        loading = true
        controller.recent(
            limit = LIMIT,
            onResult = {
                loading = false
                setSessions(it)
            },
            onError = {
                loading = false
            },
        )
    }

    internal fun setSessions(sessions: List<SessionDto>) {
        list.setSessions(sessions)
        recent.isVisible = list.count() > 0
        revalidate()
        repaint()
    }

    internal fun recentCount() = list.count()

    internal fun selectRecent(index: Int) {
        list.select(index)
    }

    internal fun selectedRecent() = list.selected()

    internal fun clickRecent(index: Int) {
        list.click(index)
    }

    internal fun recentVisible() = recent.isVisible

    internal fun explanationMarkdown() = md.markdown()

    internal fun contentPreferredSize() = content.preferredSize

    override fun dispose() {
        // no-op
    }
}
