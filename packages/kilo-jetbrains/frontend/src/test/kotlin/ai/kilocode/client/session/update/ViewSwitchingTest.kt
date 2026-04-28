package ai.kilocode.client.session.update

import kotlinx.coroutines.CompletableDeferred

class ViewSwitchingTest : SessionControllerTestBase() {

    fun `test first prompt shows messages view`() {
        val m = controller()
        val events = collect(m)
        flush()
        events.clear()

        edt { m.prompt("hello") }
        flush()

        assertControllerEvents("ViewChanged session", events)
        assertSession(
            """
            [app: DISCONNECTED] [workspace: PENDING]
            """,
            m,
        )
    }

    fun `test ViewChanged not fired twice`() {
        val m = controller()
        val events = collect(m)
        flush()
        events.clear()

        edt { m.prompt("first") }
        flush()
        edt { m.prompt("second") }
        flush()

        assertControllerEvents("ViewChanged session", events)
    }

    fun `test recent sessions show after workspace ready`() {
        projectRpc.state.value = workspaceReady()
        rpc.recent.add(session("ses_1"))
        val m = controller()
        val events = collect(m)

        flush()

        assertTrue(rpc.recentCalls.contains("/test" to SessionController.RECENT_LIMIT))
        assertControllerEvents("""
            AppChanged
            WorkspaceChanged
            WorkspaceReady
            ViewChanged recents=1
        """, events)
    }

    fun `test recent load failure shows empty recents`() {
        projectRpc.state.value = workspaceReady()
        rpc.recentFailures = 1
        val m = controller()
        val events = collect(m)

        flush()

        assertTrue(rpc.recentCalls.contains("/test" to SessionController.RECENT_LIMIT))
        assertControllerEvents("""
            AppChanged
            WorkspaceChanged
            WorkspaceReady
            ViewChanged recents=0
        """, events)
    }

    fun `test empty history transitions to recents`() {
        rpc.recent.add(session("ses_1"))
        val m = controller("ses_test", displayMs = 1_000)
        val events = collect(m)

        flush()

        assertControllerEvents("""
            AppChanged
            WorkspaceChanged
            ViewChanged recents=1
        """, events)
    }

    fun `test slow recents show progress after delay then recents`() {
        projectRpc.state.value = workspaceReady()
        rpc.recent.add(session("ses_1"))
        val gate = CompletableDeferred<Unit>()
        rpc.recentGate = gate
        val m = controller(displayMs = 50)
        val events = collect(m)

        pause(20)
        assertTrue(rpc.recentCalls.contains("/test" to SessionController.RECENT_LIMIT))
        assertFalse(events.any { it is SessionControllerEvent.ViewChanged.ShowProgress })

        pause(80)
        assertTrue(events.any { it is SessionControllerEvent.ViewChanged.ShowProgress })

        gate.complete(Unit)
        flush()

        assertEquals(
            """
            ViewChanged progress
            ViewChanged recents=1
            """.trimIndent().trim(),
            events.filterIsInstance<SessionControllerEvent.ViewChanged>().joinToString("\n"),
        )
    }

    fun `test fast recents suppress progress`() {
        projectRpc.state.value = workspaceReady()
        rpc.recent.add(session("ses_1"))
        val m = controller(displayMs = 1_000)
        val events = collect(m)

        flush()

        assertTrue(rpc.recentCalls.contains("/test" to SessionController.RECENT_LIMIT))
        assertFalse(events.any { it is SessionControllerEvent.ViewChanged.ShowProgress })
        assertTrue(events.any { it is SessionControllerEvent.ViewChanged.ShowRecents })
    }

    fun `test failed fast recents suppress progress and show empty recents`() {
        projectRpc.state.value = workspaceReady()
        rpc.recentFailures = 1
        val m = controller(displayMs = 1_000)
        val events = collect(m)

        flush()

        assertFalse(events.any { it is SessionControllerEvent.ViewChanged.ShowProgress })
        assertEquals(1, events.count { it is SessionControllerEvent.ViewChanged.ShowRecents })
        assertTrue(events.filterIsInstance<SessionControllerEvent.ViewChanged.ShowRecents>().single().recents.isEmpty())
    }

    fun `test recents progress is canceled when messages view appears`() {
        projectRpc.state.value = workspaceReady()
        rpc.recent.add(session("ses_1"))
        val gate = CompletableDeferred<Unit>()
        rpc.recentGate = gate
        val m = controller(displayMs = 50)
        val events = collect(m)

        pause(20)
        edt { m.prompt("hello") }
        pause(80)
        gate.complete(Unit)
        flush()

        assertTrue(events.any { it is SessionControllerEvent.ViewChanged.ShowSession })
        assertFalse(events.any { it is SessionControllerEvent.ViewChanged.ShowProgress })
        assertFalse(events.any { it is SessionControllerEvent.ViewChanged.ShowRecents })
    }

    private fun session(id: String) = ai.kilocode.rpc.dto.SessionDto(
        id = id,
        projectID = "prj",
        directory = "/test",
        title = "Title $id",
        version = "1",
        time = ai.kilocode.rpc.dto.SessionTimeDto(created = 1.0, updated = 2.0),
    )
}
