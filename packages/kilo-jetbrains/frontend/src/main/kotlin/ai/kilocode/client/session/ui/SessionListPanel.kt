package ai.kilocode.client.session.ui

import ai.kilocode.client.plugin.KiloBundle
import ai.kilocode.rpc.dto.SessionDto
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import kotlin.math.abs

class SessionListPanel(
    private val limit: Int,
    private val open: (SessionDto) -> Unit,
) : JPanel(BorderLayout()) {
    companion object {
        private const val MINUTE = 60_000L
        private const val HOUR = 60 * MINUTE
        private const val DAY = 24 * HOUR
    }

    private val model = DefaultListModel<SessionDto>()
    private var hover = -1

    private val list = JBList(model).apply {
        isOpaque = false
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        visibleRowCount = limit
        cellRenderer = SessionRenderer()
        emptyText.clear()
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index = row(e)
                if (index < 0) return
                selectedIndex = index
                open(model.getElementAt(index))
            }

            override fun mouseExited(e: MouseEvent) {
                hover = -1
                repaint()
            }
        })
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val index = row(e)
                if (hover == index) return
                hover = index
                repaint()
            }
        })
    }

    init {
        isOpaque = false
        add(list, BorderLayout.CENTER)
    }

    fun setSessions(sessions: List<SessionDto>) {
        model.clear()
        sessions.take(limit).forEach(model::addElement)
        revalidate()
        repaint()
    }

    fun count() = model.size()

    internal fun select(index: Int) {
        list.selectedIndex = index
    }

    internal fun selected() = list.selectedIndex

    internal fun click(index: Int) {
        list.selectedIndex = index
        open(model.getElementAt(index))
    }

    internal fun text(session: SessionDto, now: Long = System.currentTimeMillis()) = time(session, now)

    internal fun normalize(value: Double): Long {
        val raw = value.toLong()
        if (abs(raw) < 10_000_000_000L) return raw * 1000
        return raw
    }

    internal fun rendererComponent(
        session: SessionDto,
        selected: Boolean = false,
        hover: Boolean = false,
    ): Component {
        val old = this.hover
        this.hover = if (hover) 0 else -1
        return list.cellRenderer.getListCellRendererComponent(list, session, 0, selected, false).also {
            this.hover = old
        }
    }

    private fun row(e: MouseEvent): Int {
        val index = list.locationToIndex(e.point)
        if (index < 0) return -1
        val box = list.getCellBounds(index, index) ?: return -1
        if (!box.contains(e.point)) return -1
        return index
    }

    private inner class SessionRenderer : JPanel(BorderLayout()), ListCellRenderer<SessionDto> {
        private val title = JBLabel()
        private val time = JBLabel()

        init {
            border = JBUI.Borders.empty(8, 8, 8, 8)
            add(title, BorderLayout.CENTER)
            add(time, BorderLayout.EAST)
        }

        override fun getListCellRendererComponent(
            list: JList<out SessionDto>,
            value: SessionDto?,
            index: Int,
            selected: Boolean,
            focus: Boolean,
        ): Component {
            val active = selected || hover == index
            isOpaque = active
            background = if (active) list.selectionBackground else list.background
            title.foreground = if (active) list.selectionForeground else UIUtil.getLabelForeground()
            time.foreground = if (active) list.selectionForeground else UIUtil.getContextHelpForeground()
            title.text = value?.let(::title) ?: ""
            time.text = value?.let { time(it) } ?: ""
            return this
        }
    }

    private fun title(session: SessionDto) =
        session.title.takeIf { it.isNotBlank() } ?: KiloBundle.message("session.tab.untitled")

    private fun time(session: SessionDto, now: Long = System.currentTimeMillis()): String {
        val ms = normalize(session.time.updated)
        val diff = (now - ms).coerceAtLeast(0)
        if (diff < MINUTE) return KiloBundle.message("session.empty.time.moments")
        if (diff < HOUR) return KiloBundle.message("session.empty.time.minutes", (diff / MINUTE).coerceAtLeast(1))
        if (diff < DAY) return KiloBundle.message("session.empty.time.hours", (diff / HOUR).coerceAtLeast(1))
        return KiloBundle.message("session.empty.time.days", (diff / DAY).coerceAtLeast(1))
    }
}
