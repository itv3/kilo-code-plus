package ai.kilocode.client.session.model

import ai.kilocode.client.session.SessionControllerEvent

class WorkspaceWatchingTest : SessionManagerTestBase() {

    fun `test workspace ready populates agents and models`() {
        val m = model()
        val events = collect(m)
        flush()

        projectRpc.state.value = workspaceReady()
        flush()

        assertEquals(1, m.model.agents.size)
        assertEquals("code", m.model.agents[0].name)
        assertEquals(1, m.model.models.size)
        assertEquals("gpt-5", m.model.models[0].id)
        assertTrue(m.model.ready)
        assertTrue(events.any { it is SessionControllerEvent.WorkspaceReady })
    }

    fun `test workspace ready sets default agent and model`() {
        val m = model()
        collect(m)
        flush()

        projectRpc.state.value = workspaceReady()
        flush()

        assertEquals("code", m.model.agent)
        assertEquals("gpt-5", m.model.model)
    }
}
