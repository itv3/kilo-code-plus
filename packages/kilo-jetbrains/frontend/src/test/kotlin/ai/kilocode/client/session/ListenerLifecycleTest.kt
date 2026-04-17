package ai.kilocode.client.session

import ai.kilocode.client.session.model.SessionModelEvent
import ai.kilocode.rpc.dto.ChatEventDto
import ai.kilocode.rpc.dto.SessionStatusDto
import com.intellij.openapi.util.Disposer

class ListenerLifecycleTest : SessionControllerTestBase() {

    fun `test listener removed on parent dispose`() {
        val m = controller()
        val disposable = Disposer.newDisposable("listener-parent")
        Disposer.register(parent, disposable)

        val events = mutableListOf<SessionControllerEvent>()
        m.addListener(disposable) { events.add(it) }

        edt { m.prompt("before") }
        flush()

        Disposer.dispose(disposable)

        edt { m.prompt("after") }
        flush()

        assertControllerEvents("""
            ViewChanged show
            AppChanged
            WorkspaceChanged
        """, events)
    }

    fun `test all listeners notified`() {
        val m = controller()
        val events1 = mutableListOf<SessionControllerEvent>()
        val events2 = mutableListOf<SessionControllerEvent>()
        val d1 = Disposer.newDisposable("l1")
        val d2 = Disposer.newDisposable("l2")
        Disposer.register(parent, d1)
        Disposer.register(parent, d2)

        m.addListener(d1) { events1.add(it) }
        m.addListener(d2) { events2.add(it) }

        edt { m.prompt("go") }
        flush()

        assertControllerEvents("""
            ViewChanged show
            AppChanged
            WorkspaceChanged
        """, events1)
        assertControllerEvents("""
            ViewChanged show
            AppChanged
            WorkspaceChanged
        """, events2)
    }

    fun `test session status busy fires StateChanged to Busy`() {
        val (_, _, modelEvents) = prompted()

        emit(ChatEventDto.SessionStatusChanged("ses_test", SessionStatusDto("busy", null)))

        assertModelEvents("StateChanged Busy", modelEvents)
    }
}
