package ai.kilocode.client.session.ui

import ai.kilocode.client.plugin.KiloBundle
import ai.kilocode.client.session.model.SessionHeaderSnapshot
import ai.kilocode.client.session.model.SessionModelEvent
import ai.kilocode.client.session.model.TimelineItem
import ai.kilocode.client.session.update.SessionController
import ai.kilocode.client.ui.UiStyle
import ai.kilocode.rpc.dto.TokensDto
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BoxLayout
import javax.swing.JPanel

class SessionHeaderPanel(
    private val controller: SessionController,
    parent: Disposable,
) : BorderLayoutPanel(), SessionStyleTarget {

    private val title = JBLabel()
    private val cost = JBLabel().apply { foreground = UiStyle.Colors.weak() }
    private val context = JBLabel().apply { foreground = UiStyle.Colors.weak() }
    private val tokens = JBLabel().apply { foreground = UiStyle.Colors.weak() }
    private val todos = JBLabel().apply { foreground = UiStyle.Colors.weak() }
    private val compact = UiStyle.Buttons.HoverIcon().apply {
        icon = AllIcons.Actions.IntentionBulb
        toolTipText = KiloBundle.message("session.header.compact.description")
        accessibleContext.accessibleName = KiloBundle.message("session.header.compact")
        addActionListener { controller.compact() }
    }
    private val timeline = TimelinePanel()
    private val top = BorderLayoutPanel()
    private val right = JPanel(FlowLayout(FlowLayout.RIGHT, UiStyle.Gap.inline(), 0)).apply {
        isOpaque = false
        add(cost)
        add(context)
        add(compact)
    }
    private val stats = JPanel(FlowLayout(FlowLayout.LEFT, UiStyle.Gap.inline(), 0)).apply {
        isOpaque = false
        add(tokens)
        add(todos)
    }
    private val stack = JPanel().apply {
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(timeline)
        add(stats)
    }
    private var style = SessionStyle.current()

    init {
        isOpaque = true
        background = UiStyle.Colors.bg()
        updateUI()

        top.add(title, BorderLayout.CENTER)
        top.add(right, BorderLayout.EAST)
        add(top, BorderLayout.NORTH)
        add(stack, BorderLayout.CENTER)

        controller.model.addListener(parent) { event ->
            when (event) {
                is SessionModelEvent.HeaderUpdated -> update(event.header)

                is SessionModelEvent.MessageAdded,
                is SessionModelEvent.MessageUpdated,
                is SessionModelEvent.MessageRemoved,
                is SessionModelEvent.ContentAdded,
                is SessionModelEvent.ContentUpdated,
                is SessionModelEvent.ContentRemoved,
                is SessionModelEvent.ContentDelta,
                is SessionModelEvent.StateChanged,
                is SessionModelEvent.DiffUpdated,
                is SessionModelEvent.TodosUpdated,
                is SessionModelEvent.SessionUpdated,
                is SessionModelEvent.Compacted,
                is SessionModelEvent.HistoryLoaded,
                is SessionModelEvent.Cleared,
                is SessionModelEvent.TurnAdded,
                is SessionModelEvent.TurnUpdated,
                is SessionModelEvent.TurnRemoved -> Unit
            }
        }

        applyStyle(style)
        update(controller.model.header)
    }

    override fun updateUI() {
        super.updateUI()
        background = UiStyle.Colors.bg()
        border = JBUI.Borders.compound(
            JBUI.Borders.customLineBottom(UiStyle.Colors.line()),
            JBUI.Borders.empty(UiStyle.Space.LG, UiStyle.Space.PAD, UiStyle.Space.SM, UiStyle.Space.PAD),
        )
    }

    fun update(header: SessionHeaderSnapshot) {
        val before = isVisible
        title.text = header.title
        title.toolTipText = header.title
        title.accessibleContext.accessibleName = header.title
        isVisible = header.visible
        if (!header.visible) {
            if (before) refresh()
            return
        }

        set(cost, money(header.cost))
        set(context, header.context?.percentage?.let { "$it%" })
        context.toolTipText = header.context?.percentage?.let {
            KiloBundle.message("session.header.context.tooltip", num(header.context.tokens), it)
        }
        set(tokens, usage(header.tokens))
        set(todos, todo(header.todos.completed, header.todos.total))

        compact.isEnabled = header.canCompact
        timeline.setItems(header.timeline)
        refresh()
    }

    override fun applyStyle(style: SessionStyle) {
        this.style = style
        title.font = style.boldUiFont
        cost.font = style.smallUiFont
        context.font = style.smallUiFont
        tokens.font = style.smallUiFont
        todos.font = style.smallUiFont
        refresh()
    }

    internal fun titleText(): String = title.text

    internal fun costText(): String = cost.text

    internal fun contextText(): String = context.text

    internal fun tokenText(): String = tokens.text

    internal fun todoText(): String = todos.text

    internal fun compactButton() = compact

    internal fun timelineCount() = timeline.count()

    internal fun timelineKinds() = timeline.kinds()

    internal fun timelineActive(index: Int) = timeline.active(index)

    private fun refresh() {
        revalidate()
        repaint()
    }
}

private fun set(label: JBLabel, value: String?) {
    val text = value.orEmpty()
    if (label.text != text) label.text = text
    val show = text.isNotEmpty()
    if (label.isVisible != show) label.isVisible = show
}

private fun money(value: Double?): String? {
    val cost = value ?: return null
    if (cost < 0.01) return "\$%.4f".format(cost)
    if (cost < 1.0) return "\$%.2f".format(cost)
    return "\$%.2f".format(cost)
}

private fun usage(value: TokensDto?): String? {
    val tokens = value ?: return null
    val input = tokens.input
    val output = tokens.output + tokens.reasoning
    val cache = tokens.cacheRead + tokens.cacheWrite
    if (input + output + cache == 0L) return null
    val parts = mutableListOf(
        KiloBundle.message("session.header.tokens"),
        KiloBundle.message("session.header.input", num(input)),
        KiloBundle.message("session.header.output", num(output)),
    )
    if (cache > 0) parts.add("${num(cache)} ${KiloBundle.message("session.header.cache")}")
    return parts.joinToString(" ")
}

private fun todo(done: Int, total: Int): String? {
    if (total <= 0) return null
    if (done >= total) return KiloBundle.message("session.header.todos.done", total)
    return KiloBundle.message("session.header.todos.progress", done, total)
}

private fun num(value: Long): String {
    val abs = kotlin.math.abs(value)
    if (abs < 1_000) return value.toString()
    if (abs < 1_000_000) return "%.1fK".format(value / 1_000.0)
    return "%.1fM".format(value / 1_000_000.0)
}

private class TimelinePanel : JPanel(FlowLayout(FlowLayout.LEFT, UiStyle.Gap.xs(), 0)) {
    private val bars = mutableListOf<TimelineBar>()

    init {
        isOpaque = false
    }

    fun setItems(items: List<TimelineItem>) {
        while (bars.size > items.size) {
            val bar = bars.removeAt(bars.lastIndex)
            remove(bar)
        }
        while (bars.size < items.size) {
            val bar = TimelineBar()
            bars.add(bar)
            add(bar)
        }
        for ((index, item) in items.withIndex()) bars[index].setItem(item)
        val show = items.isNotEmpty()
        if (isVisible != show) isVisible = show
        revalidate()
        repaint()
    }

    fun count() = bars.size

    fun kinds() = bars.map { it.kind }

    fun active(index: Int) = bars[index].active
}

private class TimelineBar : JPanel() {
    var kind: String = ""
        private set
    var active: Boolean = false
        private set
    private var error: Boolean = false

    init {
        isOpaque = false
    }

    fun setItem(item: TimelineItem) {
        kind = item.kind
        active = item.active
        error = item.kind == "error"
        toolTipText = item.title
        preferredSize = JBUI.size((item.weight * 12).coerceAtLeast(12), 4)
        minimumSize = preferredSize
        maximumSize = preferredSize
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = when {
                error -> UiStyle.Colors.error()
                active -> UiStyle.Colors.running()
                else -> UIUtil.getBoundsColor()
            }
            val arc = JBUI.scale(4)
            g2.fillRoundRect(0, 0, width, height.coerceAtLeast(JBUI.scale(4)), arc, arc)
        } finally {
            g2.dispose()
        }
    }
}
