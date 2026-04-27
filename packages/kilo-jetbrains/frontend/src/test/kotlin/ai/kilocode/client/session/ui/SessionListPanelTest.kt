package ai.kilocode.client.session.ui

import ai.kilocode.rpc.dto.SessionDto
import ai.kilocode.rpc.dto.SessionTimeDto
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.BorderLayout
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class SessionListPanelTest : BasePlatformTestCase() {
    fun `test sessions are capped by constructor limit`() {
        val panel = panel(limit = 5)

        panel.setSessions((1..7).map { session("ses_$it") })

        assertEquals(5, panel.count())
    }

    fun `test selecting recent session does not invoke callback`() {
        val clicked = mutableListOf<String>()
        val panel = panel { clicked.add(it.id) }

        panel.setSessions(listOf(session("ses_1"), session("ses_2")))
        panel.select(1)

        assertEquals(1, panel.selected())
        assertEquals(emptyList<String>(), clicked)
    }

    fun `test clicking recent session invokes callback`() {
        val clicked = mutableListOf<String>()
        val panel = panel { clicked.add(it.id) }

        panel.setSessions(listOf(session("ses_1"), session("ses_2")))
        panel.click(1)

        assertEquals(listOf("ses_2"), clicked)
    }

    fun `test renderer aligns title center and time east`() {
        val cell = panel().rendererComponent(session("ses_1")) as JPanel
        val layout = cell.layout as BorderLayout

        assertNotNull(layout.getLayoutComponent(BorderLayout.CENTER))
        assertNotNull(layout.getLayoutComponent(BorderLayout.EAST))
    }

    fun `test hover uses selection colors`() {
        val panel = panel()
        val session = session("ses_1")
        val selected = panel.rendererComponent(session, selected = true) as JPanel
        val hovered = panel.rendererComponent(session, hover = true) as JPanel

        assertTrue(selected.isOpaque)
        assertTrue(hovered.isOpaque)
        assertEquals(selected.background, hovered.background)
    }

    fun `test timestamp normalization handles seconds and milliseconds`() {
        val panel = panel()

        assertEquals(1_700_000_000_000L, panel.normalize(1_700_000_000.0))
        assertEquals(1_700_000_000_000L, panel.normalize(1_700_000_000_000.0))
    }

    fun `test timestamp renders coarse relative text`() {
        val panel = panel()
        val now = 1_700_000_000_000L

        assertEquals("Moments ago", panel.text(session("ses_1", now - 30_000), now))
        assertEquals("2 min ago", panel.text(session("ses_1", now - 120_000), now))
        assertEquals("3h ago", panel.text(session("ses_1", now - 10_800_000), now))
        assertEquals("4d ago", panel.text(session("ses_1", now - 345_600_000), now))
    }

    private fun panel(
        limit: Int = 5,
        open: (SessionDto) -> Unit = {},
    ) = SessionListPanel(limit, open)

    private fun session(id: String, updated: Long = 2_000L) = SessionDto(
        id = id,
        projectID = "prj",
        directory = "/repo/$id",
        title = "Title $id",
        version = "1",
        time = SessionTimeDto(created = 1.0, updated = updated.toDouble()),
    )
}
