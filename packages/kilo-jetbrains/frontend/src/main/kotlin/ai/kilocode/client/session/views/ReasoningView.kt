package ai.kilocode.client.session.views

import ai.kilocode.client.plugin.KiloBundle
import ai.kilocode.client.session.model.Content
import ai.kilocode.client.session.model.Reasoning
import ai.kilocode.client.ui.md.MdView
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.border.MatteBorder

/** Renders reasoning as a VS Code-style collapsible block. */
class ReasoningView(reasoning: Reasoning) : PartView() {

    override val contentId: String = reasoning.id

    val md: MdView = MdView.html()
    private val preview: MdView = MdView.html()

    private val weak = UIUtil.getContextHelpForeground()
    private val fill = JBColor.lazy {
        UIManager.getColor("TextField.background") ?: UIUtil.getPanelBackground()
    }
    private val line = JBColor.border()

    private val arrow = JBLabel()
    private val body = JPanel(BorderLayout()).apply {
        isOpaque = true
        background = fill
        border = JBUI.Borders.empty(8, 10)
    }
    private val previewBody = JPanel(BorderLayout()).apply {
        isOpaque = true
        background = fill
        border = JBUI.Borders.empty(8, 10)
    }
    private val header = JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
        isOpaque = true
        background = fill
        border = JBUI.Borders.empty(8, 8)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
    private val title = JBLabel(KiloBundle.message("session.part.reasoning")).apply {
        foreground = weak
        font = JBUI.Fonts.smallFont()
    }
    private val icon = JBLabel(AllIcons.General.InspectionsEye).apply {
        foreground = weak
    }

    private var done = reasoning.done
    private var open = false
    private var touched = false
    private var source = reasoning.content.toString()

    private val click = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (!canExpand()) return
            touched = true
            setOpen(!open)
        }
    }

    init {
        layout = BorderLayout()
        isOpaque = false
        border = JBUI.Borders.compound(
            MatteBorder(0, JBUI.scale(2), 0, 0, line),
            JBUI.Borders.empty(0, 0, 0, 0),
        )

        val left = JPanel(BorderLayout(JBUI.scale(6), 0)).apply {
            isOpaque = false
            add(icon, BorderLayout.WEST)
            add(title, BorderLayout.CENTER)
        }

        header.add(left, BorderLayout.CENTER)
        header.add(arrow, BorderLayout.EAST)
        header.addMouseListener(click)
        left.addMouseListener(click)
        title.addMouseListener(click)
        icon.addMouseListener(click)
        arrow.addMouseListener(click)

        md.foreground = weak
        md.opaque = false
        preview.foreground = weak
        preview.opaque = false
        body.add(md.component, BorderLayout.CENTER)
        previewBody.add(preview.component, BorderLayout.CENTER)

        add(header, BorderLayout.NORTH)
        setText(source)
        render()
    }

    override fun update(content: Content) {
        if (content !is Reasoning) return
        done = content.done
        source = content.content.toString()
        setText(source)
        if (!touched) open = false
        render()
    }

    override fun appendDelta(delta: String) {
        source += delta
        md.append(delta)
        preview.set(preview(source))
        if (!touched) open = false
        render()
    }

    fun markdown(): String = source

    fun previewMarkdown(): String = preview.markdown()

    fun isExpanded(): Boolean = open

    fun hasToggle(): Boolean = arrow.isVisible

    fun headerText(): String = title.text

    fun toggle() {
        if (!canExpand()) return
        touched = true
        setOpen(!open)
    }

    private fun setOpen(value: Boolean) {
        open = value
        render()
    }

    private fun render() {
        val expand = canExpand()
        if (!expand) open = false
        arrow.isVisible = expand
        arrow.icon = if (open) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
        val cursor = if (expand) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor()
        header.cursor = cursor
        title.cursor = cursor
        icon.cursor = cursor
        arrow.cursor = cursor
        remove(body)
        remove(previewBody)
        if (open && expand) {
            add(body, BorderLayout.CENTER)
        } else if (source.isNotBlank()) {
            add(previewBody, BorderLayout.CENTER)
        }
        revalidate()
        repaint()
    }

    private fun setText(text: String) {
        md.set(text)
        preview.set(preview(text))
    }

    private fun canExpand(): Boolean = lines(source) > PREVIEW_LINES

    override fun dumpLabel() = "ReasoningView#$contentId(${if (open) "open" else "closed"})"
}

private const val PREVIEW_LINES = 3

private fun preview(text: String): String = text.lineSequence().take(PREVIEW_LINES).joinToString("\n")

private fun lines(text: String): Int = if (text.isBlank()) 0 else text.lineSequence().count()
