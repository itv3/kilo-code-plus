package ai.kilocode.client.session

import ai.kilocode.client.session.model.SessionState
import ai.kilocode.rpc.dto.PermissionRequestDto
import ai.kilocode.rpc.dto.QuestionInfoDto
import ai.kilocode.rpc.dto.QuestionRequestDto
import ai.kilocode.rpc.dto.SessionTimeDto

/**
 * Tests for pending permission/question recovery after history load.
 *
 * VS Code rehydrates pending prompts by calling list endpoints after
 * reconnect. JetBrains now does the same in [SessionController.recoverPending].
 */
class SessionRecoveryTest : SessionControllerTestBase() {

    override fun setUp() {
        super.setUp()
        // Set a pre-existing session in the fake API
        rpc.session = rpc.session.copy(id = "ses_test")
    }

    fun `test pending permission is recovered on history load`() {
        rpc.pendingPermissionList.add(
            PermissionRequestDto(
                id = "perm_pending",
                sessionID = "ses_test",
                permission = "read",
                patterns = listOf("*.json"),
            )
        )

        appRpc.state.value = ai.kilocode.rpc.dto.KiloAppStateDto(ai.kilocode.rpc.dto.KiloAppStatusDto.READY)
        projectRpc.state.value = workspaceReady()
        val m = controller("ses_test")
        flush()

        assertTrue(m.model.state is SessionState.AwaitingPermission)
        val perm = (m.model.state as SessionState.AwaitingPermission).permission
        assertEquals("perm_pending", perm.id)
        assertEquals("read", perm.name)
    }

    fun `test pending question is recovered when no pending permissions`() {
        rpc.pendingQuestionList.add(
            QuestionRequestDto(
                id = "q_pending",
                sessionID = "ses_test",
                questions = listOf(QuestionInfoDto("What?", "Q")),
            )
        )

        appRpc.state.value = ai.kilocode.rpc.dto.KiloAppStateDto(ai.kilocode.rpc.dto.KiloAppStatusDto.READY)
        projectRpc.state.value = workspaceReady()
        val m = controller("ses_test")
        flush()

        assertTrue(m.model.state is SessionState.AwaitingQuestion)
        val q = (m.model.state as SessionState.AwaitingQuestion).question
        assertEquals("q_pending", q.id)
    }

    fun `test pending from other session is ignored`() {
        rpc.pendingPermissionList.add(
            PermissionRequestDto(
                id = "perm_other",
                sessionID = "ses_other",  // different session
                permission = "read",
                patterns = emptyList(),
            )
        )

        appRpc.state.value = ai.kilocode.rpc.dto.KiloAppStateDto(ai.kilocode.rpc.dto.KiloAppStatusDto.READY)
        projectRpc.state.value = workspaceReady()
        val m = controller("ses_test")
        flush()

        // State should remain Idle — other session's pending is irrelevant
        assertEquals(SessionState.Idle, m.model.state)
    }

    fun `test permission takes priority over question in recovery`() {
        rpc.pendingPermissionList.add(
            PermissionRequestDto(
                id = "perm_pending",
                sessionID = "ses_test",
                permission = "edit",
                patterns = emptyList(),
            )
        )
        rpc.pendingQuestionList.add(
            QuestionRequestDto(
                id = "q_pending",
                sessionID = "ses_test",
                questions = listOf(QuestionInfoDto("What?", "Q")),
            )
        )

        appRpc.state.value = ai.kilocode.rpc.dto.KiloAppStateDto(ai.kilocode.rpc.dto.KiloAppStatusDto.READY)
        projectRpc.state.value = workspaceReady()
        val m = controller("ses_test")
        flush()

        // Permission list non-empty → AwaitingPermission wins
        assertTrue(m.model.state is SessionState.AwaitingPermission)
    }
}
