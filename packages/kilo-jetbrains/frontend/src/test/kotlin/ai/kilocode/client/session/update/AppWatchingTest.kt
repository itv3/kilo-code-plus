package ai.kilocode.client.session.update

import ai.kilocode.rpc.dto.ConfigWarningDto
import ai.kilocode.rpc.dto.KiloWorkspaceStatusDto
import ai.kilocode.rpc.dto.KiloAppStateDto
import ai.kilocode.rpc.dto.KiloAppStatusDto

class AppWatchingTest : SessionControllerTestBase() {

    fun `test app state change fires AppChanged`() {
        val m = controller()
        val events = collect(m)
        flush()
        events.clear()

        appRpc.state.value = KiloAppStateDto(KiloAppStatusDto.READY)
        flush()

        assertControllerEvents("AppChanged", events)
        assertSession(
            """
            [app: READY] [workspace: PENDING]
            """,
            m,
            show = false,
        )
    }

    fun `test retry connection uses app retry when app is failed`() {
        val m = controller()
        appRpc.state.value = KiloAppStateDto(KiloAppStatusDto.ERROR, error = "boom")

        flush()
        edt { m.retryConnection() }
        flush()

        assertEquals(1, appRpc.retries)
        assertEquals(0, projectRpc.reloads)
    }

    fun `test retry connection reloads workspace when app ready and workspace failed`() {
        val m = controller()
        appRpc.state.value = KiloAppStateDto(KiloAppStatusDto.READY)
        projectRpc.state.value = ai.kilocode.rpc.dto.KiloWorkspaceStateDto(
            status = KiloWorkspaceStatusDto.ERROR,
            error = "workspace fail",
        )

        flush()
        edt { m.retryConnection() }
        flush()

        assertEquals(0, appRpc.retries)
        assertEquals(1, projectRpc.reloads)
    }

    fun `test retry connection uses app retry when app has warnings`() {
        val m = controller()
        appRpc.state.value = KiloAppStateDto(
            status = KiloAppStatusDto.READY,
            warnings = listOf(ConfigWarningDto(path = ".kilo/kilo.json", message = "Invalid JSON")),
        )

        flush()
        edt { m.retryConnection() }
        flush()

        assertEquals(1, appRpc.retries)
        assertEquals(0, projectRpc.reloads)
    }
}
