package ai.kilocode.client.session.model

import ai.kilocode.rpc.dto.ChatEventDto
import ai.kilocode.rpc.dto.MessageErrorDto

class TurnLifecycleTest : SessionManagerTestBase() {

    fun `test TurnOpen fires PhaseChanged to Working`() {
        val (_, _, model) = prompted()

        emit(ChatEventDto.TurnOpen("ses_test"))
        flush()

        val phase = model.filterIsInstance<SessionModelEvent.PhaseChanged>().lastOrNull()?.phase
        assertTrue(phase is SessionPhase.Working)
    }

    fun `test TurnClose fires PhaseChanged to Idle`() {
        val (_, _, model) = prompted()

        emit(ChatEventDto.TurnOpen("ses_test"))
        flush()
        emit(ChatEventDto.TurnClose("ses_test", "completed"))
        flush()

        val phase = model.filterIsInstance<SessionModelEvent.PhaseChanged>().last().phase
        assertEquals(SessionPhase.Idle, phase)
    }

    fun `test Error fires PhaseChanged to Error`() {
        val (_, _, model) = prompted()

        emit(ChatEventDto.Error("ses_test", MessageErrorDto(type = "APIError", message = "Bad Request")))
        flush()

        val phase = model.filterIsInstance<SessionModelEvent.PhaseChanged>().last().phase
        assertTrue(phase is SessionPhase.Error)
        assertEquals("Bad Request", (phase as SessionPhase.Error).message)
    }

    fun `test Error with null message falls back to type`() {
        val (_, _, model) = prompted()

        emit(ChatEventDto.Error("ses_test", MessageErrorDto(type = "timeout", message = null)))
        flush()

        val phase = model.filterIsInstance<SessionModelEvent.PhaseChanged>().last().phase as SessionPhase.Error
        assertEquals("timeout", phase.message)
    }
}
