package ai.kilocode.client.session.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color

/** Static UI tokens and helpers for session-specific Swing surfaces. */
object SessionUiStyle {
    object Timeline {
        val read: Color = JBColor(Color(0x37, 0x94, 0xff), Color(0x37, 0x94, 0xff))
        val write: Color = JBColor(Color(0x00, 0x7f, 0xd4), Color(0x00, 0x7f, 0xd4))
        val tool: Color = JBColor(Color(0x00, 0x7a, 0xcc), Color(0x00, 0x7a, 0xcc))
        val success: Color = JBColor.namedColor("Label.successForeground", UIUtil.getLabelSuccessForeground())
        val error: Color = JBColor(Color(0xf4, 0x87, 0x71), Color(0xf4, 0x87, 0x71))
        val text: Color = JBColor(Color(0x9d, 0x9d, 0x9d), Color(0x9d, 0x9d, 0x9d))
        val step: Color = JBColor(Color(0x4d, 0x4d, 0x4d), Color(0x4d, 0x4d, 0x4d))
    }
}
