@file:Suppress("TooManyFunctions")

package ai.kilocode.client.session.views

import ai.kilocode.client.plugin.KiloBundle
import ai.kilocode.client.session.model.Content
import ai.kilocode.client.session.model.Reasoning
import ai.kilocode.client.session.ui.SessionStyle
import ai.kilocode.client.ui.UiStyle
import ai.kilocode.client.ui.md.MdView
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.Scrollable
import javax.swing.SwingUtilities

/** Renders reasoning as a VS Code-style collapsible block. */
class ReasoningView(reasoning: Reasoning) : PartView() {

    override val contentId: String = reasoning.id

    val md: MdView get() = body().md

    private val arrow = JBLabel()
    private val header = JPanel(UiStyle.Card.layout()).apply {
        isOpaque = true
        background = UiStyle.Colors.header()
        border = UiStyle.Card.headerInsets()
    }
    private val title = JBLabel(KiloBundle.message("session.part.reasoning")).apply {
        foreground = UiStyle.Colors.weak()
    }
    private val icon = JBLabel(AllIcons.General.InspectionsEye).apply {
        foreground = UiStyle.Colors.weak()
    }

    private var style = SessionStyle.current()
    private var source = reasoning.content.toString()
    private var body: ReasoningBody? = null

    private val click = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (!canExpand()) return
            toggle()
        }
    }

    private val mouse = object : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
            setHover(true)
        }

        override fun mouseExited(e: MouseEvent) {
            if (inside(e)) return
            setHover(false)
        }
    }

    init {
        layout = BorderLayout()
        isOpaque = false
        border = UiStyle.Card.border()

        val left = JPanel(UiStyle.Card.layout()).apply {
            isOpaque = false
            add(icon, BorderLayout.WEST)
            add(title, BorderLayout.CENTER)
        }

        header.add(left, BorderLayout.CENTER)
        header.add(arrow, BorderLayout.EAST)
        listOf(header, left, title, icon, arrow).forEach {
            it.addMouseListener(click)
            it.addMouseListener(mouse)
        }

        applyStyle(SessionStyle.current())
        add(header, BorderLayout.NORTH)
        sync()
    }

    override fun update(content: Content) {
        if (content !is Reasoning) return
        var changed = false
        val next = content.content.toString()
        if (source != next) {
            source = next
            body?.md?.set(source)
            changed = true
        }
        if (content.done) changed = collapse() || changed
        changed = sync() || changed
        if (changed) refresh()
    }

    override fun appendDelta(delta: String) {
        if (delta.isEmpty()) return
        source += delta
        body?.md?.append(delta)
        val changed = sync()
        if (changed || bodyVisible()) refresh()
    }

    fun markdown(): String = source

    fun isExpanded(): Boolean = bodyVisible()

    fun hasToggle(): Boolean = arrow.isVisible

    fun headerText(): String = title.text

    internal fun headerFont() = title.font

    internal fun bodyVisible() = body?.scroll?.parent === this

    internal fun horizontalPolicy() = body?.scroll?.horizontalScrollBarPolicy
        ?: ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

    internal fun bodyMaxRows() = UiStyle.Card.REASONING_LINES

    internal fun bodyCreated() = body != null

    override fun applyStyle(style: SessionStyle) {
        this.style = style
        var changed = false
        if (title.font != style.smallEditorFont) {
            title.font = style.smallEditorFont
            changed = true
        }
        body?.let {
            changed = apply(it.md) || changed
        }
        if (changed) refresh()
    }

    fun toggle() {
        if (!canExpand()) return
        var changed = if (bodyVisible()) collapse() else expand()
        changed = sync() || changed
        if (changed) refresh()
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        if (!bodyVisible()) return size
        val height = header.preferredSize.height + bodyMaxHeight()
        return Dimension(size.width, minOf(size.height, height))
    }

    private fun setHover(value: Boolean) {
        val color = if (value) UiStyle.Colors.headerHover() else UiStyle.Colors.header()
        if (header.background?.rgb == color.rgb) return
        header.background = color
        header.repaint()
    }

    private fun inside(e: MouseEvent): Boolean {
        val point = SwingUtilities.convertPoint(e.component, e.point, header)
        return header.contains(point)
    }

    private fun canExpand(): Boolean = source.isNotBlank()

    private fun sync(): Boolean {
        val expand = canExpand()
        if (!expand) collapse()
        var changed = false
        changed = setVisible(arrow, expand) || changed
        changed = syncArrow() || changed
        val cursor = if (expand) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor()
        listOf(header, title, icon, arrow).forEach {
            if (it.cursor?.type != cursor.type) {
                it.cursor = cursor
                changed = true
            }
        }
        return changed
    }

    private fun setVisible(component: JBLabel, visible: Boolean): Boolean {
        if (component.isVisible == visible) return false
        component.isVisible = visible
        return true
    }

    private fun setIcon(label: JBLabel, icon: javax.swing.Icon): Boolean {
        if (label.icon === icon) return false
        label.icon = icon
        return true
    }

    private fun syncArrow(): Boolean {
        val icon = if (bodyVisible()) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
        return setIcon(arrow, icon)
    }

    private fun expand(): Boolean {
        if (bodyVisible()) return false
        val view = body()
        add(view.scroll, BorderLayout.CENTER)
        return true
    }

    private fun collapse(): Boolean {
        val view = body
        val attached = view?.scroll?.parent === this
        if (attached) remove(view.scroll)
        return attached
    }

    private fun body(): ReasoningBody {
        body?.let { return it }
        val md = MdView.html()
        md.opaque = false
        apply(md)
        md.set(source)
        val panel = TrackPanel().apply {
            isOpaque = true
            background = UiStyle.Colors.surface()
            border = UiStyle.Card.bodyInsets()
            add(md.component, BorderLayout.CENTER)
        }
        val scroll = JBScrollPane(panel).apply {
            border = UiStyle.Card.divider()
            isOpaque = true
            background = UiStyle.Colors.surface()
            viewport.background = UiStyle.Colors.surface()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        val view = ReasoningBody(md, scroll)
        body = view
        return view
    }

    private fun apply(md: MdView): Boolean {
        var changed = false
        val font = style.smallEditorFont.deriveFont(Font.ITALIC)
        changed = md.font != font || changed
        md.font = font
        changed = md.codeFont != style.editorFamily || changed
        md.codeFont = style.editorFamily
        changed = md.foreground.rgb != UiStyle.Colors.weak().rgb || changed
        md.foreground = UiStyle.Colors.weak()
        return changed
    }

    private fun refresh() {
        revalidate()
        repaint()
    }

    private fun bodyMaxHeight(): Int = md.component.getFontMetrics(md.font).height * bodyMaxRows() +
        UiStyle.Card.scrollChrome()

    override fun dumpLabel(): String {
        val state = if (bodyVisible()) "open" else "closed"
        return "ReasoningView#$contentId($state)"
    }
}

private data class ReasoningBody(
    val md: MdView,
    val scroll: JBScrollPane,
)

private class TrackPanel : JPanel(BorderLayout()), Scrollable {
    override fun getScrollableTracksViewportWidth() = true
    override fun getScrollableTracksViewportHeight() = false
    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
    override fun getScrollableUnitIncrement(
        visibleRect: Rectangle,
        orientation: Int,
        direction: Int,
    ) = UiStyle.Gap.scroll()
    override fun getScrollableBlockIncrement(
        visibleRect: Rectangle,
        orientation: Int,
        direction: Int,
    ) = visibleRect.height
}
