package ai.kilocode.client.session.ui

import ai.kilocode.client.app.KiloAppService
import ai.kilocode.client.app.KiloSessionService
import ai.kilocode.client.app.KiloWorkspaceService
import ai.kilocode.client.app.Workspace
import ai.kilocode.client.session.update.SessionController
import ai.kilocode.client.session.update.SessionControllerEvent
import ai.kilocode.client.testing.FakeAppRpcApi
import ai.kilocode.client.testing.FakeSessionRpcApi
import ai.kilocode.client.testing.FakeWorkspaceRpcApi
import ai.kilocode.rpc.dto.KiloAppStateDto
import ai.kilocode.rpc.dto.KiloAppStatusDto
import ai.kilocode.rpc.dto.KiloWorkspaceStateDto
import ai.kilocode.rpc.dto.KiloWorkspaceStatusDto
import ai.kilocode.rpc.dto.SessionDto
import ai.kilocode.rpc.dto.SessionTimeDto
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@Suppress("UnstableApiUsage")
class EmptySessionPanelTest : BasePlatformTestCase() {
    private lateinit var scope: CoroutineScope
    private lateinit var rpc: FakeSessionRpcApi
    private lateinit var app: KiloAppService
    private lateinit var workspace: Workspace
    private lateinit var controller: SessionController

    override fun setUp() {
        super.setUp()
        scope = CoroutineScope(SupervisorJob())
        rpc = FakeSessionRpcApi()
        app = KiloAppService(scope, FakeAppRpcApi().also {
            it.state.value = KiloAppStateDto(KiloAppStatusDto.READY)
        })
        val workspaces = KiloWorkspaceService(scope, FakeWorkspaceRpcApi().also {
            it.state.value = KiloWorkspaceStateDto(KiloWorkspaceStatusDto.READY)
        })
        workspace = workspaces.workspace("/test")
        controller = SessionController(
            parent = testRootDisposable,
            id = null,
            sessions = KiloSessionService(project, scope, rpc),
            workspace = workspace,
            app = app,
            cs = scope,
        )
    }

    override fun tearDown() {
        try {
            scope.cancel()
        } finally {
            super.tearDown()
        }
    }

    fun `test recent section is hidden when empty`() {
        val panel = panel()

        panel.setSessions(emptyList())

        assertFalse(panel.recentVisible())
        assertEquals(0, panel.recentCount())
    }

    fun `test empty state has visible preferred height`() {
        val panel = panel()

        assertTrue(panel.preferredSize.height > 0)
    }

    fun `test content has fixed preferred width`() {
        val panel = panel()

        assertEquals(com.intellij.util.ui.JBUI.scale(EmptySessionPanel.MAX_WIDTH), panel.contentPreferredSize().width)
        assertTrue(panel.contentPreferredSize().height > 0)
    }

    fun `test recent sessions are capped at five`() {
        val panel = panel()

        panel.setSessions((1..7).map { session("ses_$it") })

        assertTrue(panel.recentVisible())
        assertEquals(5, panel.recentCount())
    }

    fun `test explanation uses markdown view`() {
        val panel = panel()

        assertEquals(
            "Kilo Code is an AI coding assistant. Ask it to build features, fix bugs, or explain your codebase.",
            panel.explanationMarkdown(),
        )
    }

    fun `test panel loads recent sessions`() {
        rpc.recent.add(session("ses_1"))
        val panel = panel()

        settle()

        assertTrue(rpc.recentCalls.contains("/test" to 5))
        assertTrue(panel.recentVisible())
        assertEquals(1, panel.recentCount())
    }

    fun `test panel retries recent sessions when controller becomes ready`() {
        rpc.recentFailures = 1
        rpc.recent.add(session("ses_1"))
        val panel = panel()

        settle()
        panel.onEvent(SessionControllerEvent.WorkspaceReady)
        settle()

        assertTrue(rpc.recentCalls.size >= 2)
        assertTrue(panel.recentVisible())
        assertEquals(1, panel.recentCount())
    }

    fun `test panel refreshes when shown after hide`() {
        rpc.recent.add(session("ses_1"))
        val panel = panel()
        settle()
        rpc.recent.clear()
        rpc.recent.add(session("ses_2"))

        panel.onEvent(SessionControllerEvent.ViewChanged(false))
        settle()

        assertTrue(rpc.recentCalls.size >= 2)
        assertEquals(1, panel.recentCount())
    }

    private fun panel(open: (SessionDto) -> Unit = {}) = EmptySessionPanel(testRootDisposable, controller, open)

    private fun settle() = runBlocking {
        repeat(5) {
            delay(100)
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents()
        }
    }

    private fun session(id: String) = SessionDto(
        id = id,
        projectID = "prj",
        directory = "/repo/$id",
        title = "Title $id",
        version = "1",
        time = SessionTimeDto(created = 1.0, updated = 2.0),
    )
}
