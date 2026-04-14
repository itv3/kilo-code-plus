package ai.kilocode.client.chat

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Chat input area with a text field and send/abort button.
 *
 * Enter sends the message. Shift+Enter inserts a newline.
 * When busy, the button changes to an abort button.
 */
class ChatInputPanel(
    private val onSend: (String) -> Unit,
    private val onAbort: () -> Unit,
) : JPanel(BorderLayout()) {

    private val area = JBTextArea(3, 40).apply {
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(4)
        emptyText.text = "Type a message..."
    }

    private val button = JButton("Send").apply {
        addActionListener { handleClick() }
    }

    @Volatile
    private var busy = false

    init {
        border = JBUI.Borders.empty(4, 8)

        area.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                    e.consume()
                    if (!busy) {
                        onSend(area.text.trim())
                    }
                }
            }
        })

        add(area, BorderLayout.CENTER)
        add(button, BorderLayout.EAST)
    }

    fun setBusy(value: Boolean) {
        busy = value
        button.text = if (value) "Stop" else "Send"
        button.icon = if (value) AllIcons.Actions.Suspend else null
    }

    fun clearInput() {
        area.text = ""
    }

    private fun handleClick() {
        if (busy) {
            onAbort()
        } else {
            val text = area.text.trim()
            if (text.isNotEmpty()) {
                onSend(text)
            }
        }
    }
}
