package ai.kilocode.client.session.ui

import ai.kilocode.client.session.update.SessionControllerTestBase
import ai.kilocode.rpc.dto.ChatEventDto
import ai.kilocode.rpc.dto.MessageDto
import ai.kilocode.rpc.dto.MessageTimeDto
import ai.kilocode.rpc.dto.ModelDto
import ai.kilocode.rpc.dto.PartDto
import ai.kilocode.rpc.dto.PartTimeDto
import ai.kilocode.rpc.dto.ProviderDto
import ai.kilocode.rpc.dto.TodoDto
import ai.kilocode.rpc.dto.TokensDto

class SessionHeaderPanelTest : SessionControllerTestBase() {

    fun `test starts hidden for empty header`() {
        appRpc.state.value = ai.kilocode.rpc.dto.KiloAppStateDto(ai.kilocode.rpc.dto.KiloAppStatusDto.READY)
        projectRpc.state.value = workspaceReady()
        val c = controller()
        flush()
        val panel = SessionHeaderPanel(c, parent)

        assertFalse(panel.isVisible)
        assertEquals("New Session", panel.titleText())
    }

    fun `test shows populated session header`() {
        val c = promptedHeader()
        val panel = SessionHeaderPanel(c, parent)

        assertTrue(panel.isVisible)
        assertFalse(panel.isExpanded())
        assertEquals("Generated title", panel.titleText())
        assertEquals("$0.07", panel.costText())
        assertEquals("1%", panel.contextText())
        assertEquals("Tokens 13.7K 2.5K cache write 25 cache read 75", panel.tokenText())
        assertEquals("13.7K", panel.inputTokenText())
        assertEquals("2.5K", panel.outputTokenText())
        assertEquals("cache read 75", panel.cacheReadText())
        assertEquals("cache write 25", panel.cacheWriteText())
        assertEquals("1/2 todos complete", panel.todoText())
        assertTrue(panel.todoVisible())
        assertNotNull(panel.expandButton().icon)
    }

    fun `test compact button follows eligibility and invokes controller`() {
        val c = promptedHeader()
        val panel = SessionHeaderPanel(c, parent)

        assertTrue(panel.compactButton().isEnabled)
        panel.compactButton().doClick()
        flush()
        assertEquals(1, rpc.compacts.size)

        emit(ChatEventDto.TurnOpen("ses_test"))
        assertFalse(panel.compactButton().isEnabled)
        panel.compactButton().doClick()
        flush()
        assertEquals(1, rpc.compacts.size)
    }

    fun `test retained labels update on later header event`() {
        val c = promptedHeader()
        val panel = SessionHeaderPanel(c, parent)
        val button = panel.compactButton()

        emit(ChatEventDto.SessionUpdated("ses_test", session("ses_test", title = "New title")))
        emit(ChatEventDto.MessageUpdated("ses_test", assistant(cost = 0.2, tokens = TokensDto(1_000, 500, 0, 0, 0))))

        assertSame(button, panel.compactButton())
        assertEquals("New title", panel.titleText())
        assertEquals("$0.20", panel.costText())
        assertEquals("Tokens 1.0K 500", panel.tokenText())
        assertEquals("1.0K", panel.inputTokenText())
        assertEquals("500", panel.outputTokenText())
    }

    fun `test expanded body shows timeline context and token metrics`() {
        val c = promptedHeader()
        val panel = SessionHeaderPanel(c, parent)
        val body = panel.bodyPanel()
        val timeline = panel.timelinePanel()
        val bar = panel.contextBar()

        assertFalse(panel.isExpanded())

        panel.expandButton().doClick()

        assertTrue(panel.isExpanded())
        assertSame(body, panel.bodyPanel())
        assertSame(timeline, panel.timelinePanel())
        assertSame(bar, panel.contextBar())
        assertEquals(3, panel.timelineCount())
        assertEquals(listOf("reasoning", "tool", "error"), panel.timelineKinds())
        assertTrue(panel.timelineActive(0))
        assertTrue(panel.timelineActive(1))
        assertFalse(panel.timelineActive(2))
        assertTrue(panel.contextBarVisible())
        assertEquals(16_300L, panel.contextBarUsed())
        assertEquals(200_000L, panel.contextBarReserved())
        assertEquals(1_783_700L, panel.contextBarAvailable())
        assertEquals(2_000_000L, panel.contextBarLimit())

        panel.expandButton().doClick()

        assertFalse(panel.isExpanded())
        assertSame(body, panel.bodyPanel())
        assertSame(timeline, panel.timelinePanel())
        assertSame(bar, panel.contextBar())
        assertEquals(3, panel.timelineCount())
    }

    private fun promptedHeader(): ai.kilocode.client.session.update.SessionController {
        appRpc.state.value = ai.kilocode.rpc.dto.KiloAppStateDto(
            ai.kilocode.rpc.dto.KiloAppStatusDto.READY,
            config = ai.kilocode.rpc.dto.ConfigDto(model = "kilo/gpt-5"),
        )
        projectRpc.state.value = workspaceReady(
            providers = listOf(
                ProviderDto(
                    id = "kilo",
                    name = "Kilo",
                    models = mapOf(
                        "gpt-5" to ModelDto(
                            id = "gpt-5",
                            name = "GPT-5",
                            limit = ai.kilocode.rpc.dto.ModelLimitDto(context = 2_000_000, output = 200_000),
                        ),
                    ),
                ),
            ),
        )
        val c = controller()
        flush()
        edt { c.prompt("go") }
        flush()

        emit(ChatEventDto.SessionUpdated("ses_test", session("ses_test", title = "Generated title")))
        emit(ChatEventDto.MessageUpdated("ses_test", assistant()))
        emit(ChatEventDto.PartUpdated("ses_test", reasoning(done = false)))
        emit(ChatEventDto.PartUpdated("ses_test", tool("tool_1", "bash", "running", "Run tests")))
        emit(ChatEventDto.PartUpdated("ses_test", tool("tool_2", "edit", "error", "Edit file")))
        emit(ChatEventDto.TodoUpdated("ses_test", listOf(
            TodoDto("Write tests", "completed", "high"),
            TodoDto("Ship it", "pending", "medium"),
        )))
        return c
    }

    private fun assistant(
        cost: Double = 0.07,
        tokens: TokensDto = TokensDto(13_700, 2_000, 500, 75, 25),
    ) = MessageDto(
        id = "msg1",
        sessionID = "ses_test",
        role = "assistant",
        time = MessageTimeDto(created = 0.0),
        cost = cost,
        tokens = tokens,
    )

    private fun reasoning(done: Boolean) = PartDto(
        id = "reasoning_1",
        sessionID = "ses_test",
        messageID = "msg1",
        type = "reasoning",
        text = "Thinking",
        time = if (done) PartTimeDto(1.0, 2.0) else PartTimeDto(1.0, null),
    )

    private fun tool(id: String, name: String, state: String, title: String) = PartDto(
        id = id,
        sessionID = "ses_test",
        messageID = "msg1",
        type = "tool",
        tool = name,
        state = state,
        title = title,
        input = mapOf("cmd" to "test"),
        time = PartTimeDto(1.0, 3.0),
    )
}
