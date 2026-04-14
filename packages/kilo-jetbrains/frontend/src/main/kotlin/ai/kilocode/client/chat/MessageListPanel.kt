package ai.kilocode.client.chat

import ai.kilocode.rpc.dto.MessageDto
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingConstants

/**
 * Scrollable panel displaying chat messages.
 *
 * Each message is rendered as a role label + text area block.
 * Supports incremental text updates via part IDs for streaming.
 */
class MessageListPanel : JPanel() {

    /** Maps messageID to the panel for that message. */
    private val panels = LinkedHashMap<String, MessageBlock>()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = true
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty(8)
    }

    fun addMessage(info: MessageDto) {
        if (panels.containsKey(info.id)) return

        val block = MessageBlock(info)
        panels[info.id] = block
        add(block)
        revalidate()
        repaint()
    }

    fun updatePartText(messageID: String, partID: String, text: String) {
        val block = panels[messageID] ?: return
        block.setText(partID, text)
    }

    fun appendDelta(messageID: String, partID: String, delta: String) {
        val block = panels[messageID] ?: return
        block.appendDelta(partID, delta)
    }

    fun removeMessage(messageID: String) {
        val block = panels.remove(messageID) ?: return
        remove(block)
        revalidate()
        repaint()
    }

    fun addError(msg: String) {
        val label = JBLabel(msg).apply {
            foreground = JBColor.RED
            font = JBUI.Fonts.label()
            border = JBUI.Borders.empty(4, 8)
            alignmentX = Component.LEFT_ALIGNMENT
        }
        add(label)
        revalidate()
        repaint()
    }

    fun clear() {
        panels.clear()
        removeAll()
        revalidate()
        repaint()
    }
}

/**
 * A single message block: role header + text content area.
 */
private class MessageBlock(info: MessageDto) : JPanel(BorderLayout()) {
    private val parts = LinkedHashMap<String, JTextArea>()
    private val body = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
    }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(6, 0)
        alignmentX = Component.LEFT_ALIGNMENT

        val role = when (info.role) {
            "user" -> "You"
            "assistant" -> "Assistant"
            else -> info.role
        }

        val header = JBLabel(role).apply {
            font = JBUI.Fonts.label().deriveFont(JBUI.Fonts.label().style or java.awt.Font.BOLD)
            foreground = when (info.role) {
                "user" -> UIUtil.getLabelForeground()
                else -> JBColor(0x4A90D9, 0x6CB4EE)
            }
            border = JBUI.Borders.empty(0, 0, 4, 0)
            horizontalAlignment = SwingConstants.LEFT
        }

        add(header, BorderLayout.NORTH)
        add(body, BorderLayout.CENTER)
    }

    fun setText(partID: String, text: String) {
        val area = parts.getOrPut(partID) { createArea().also { body.add(it) } }
        area.text = text
        body.revalidate()
    }

    fun appendDelta(partID: String, delta: String) {
        val area = parts.getOrPut(partID) { createArea().also { body.add(it) } }
        area.append(delta)
        body.revalidate()
    }

    private fun createArea() = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        isOpaque = false
        font = JBUI.Fonts.label()
        foreground = UIUtil.getLabelForeground()
        border = JBUI.Borders.empty()
        alignmentX = Component.LEFT_ALIGNMENT
    }
}
