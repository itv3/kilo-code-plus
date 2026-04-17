package ai.kilocode.client.session

import ai.kilocode.client.session.model.SessionModelEvent
import ai.kilocode.rpc.dto.ChatEventDto

class MessageListTest : SessionControllerTestBase() {

    fun `test MessageUpdated adds message to SessionModel`() {
        val (m, _, modelEvents) = prompted()

        emit(ChatEventDto.MessageUpdated("ses_test", msg("msg1", "ses_test", "assistant")))

        assertModelEvents("MessageAdded msg1", modelEvents)
        assertNotNull(m.model.message("msg1"))
    }

    fun `test PartUpdated text updates SessionModel`() {
        val (m, _, modelEvents) = prompted()

        emit(ChatEventDto.MessageUpdated("ses_test", msg("msg1", "ses_test", "assistant")))

        emit(ChatEventDto.PartUpdated("ses_test", part("prt1", "ses_test", "msg1", "text", text = "hello")))

        assertModelEvents("""
            MessageAdded msg1
            ContentAdded msg1/prt1
        """, modelEvents)
        assertModel(
            """
            assistant#msg1
            text#prt1:
              hello
            """,
            m,
        )
    }

    fun `test PartDelta appends text to SessionModel`() {
        val (m, _, _) = prompted()

        emit(ChatEventDto.MessageUpdated("ses_test", msg("msg1", "ses_test", "assistant")))

        emit(ChatEventDto.PartDelta("ses_test", "msg1", "prt1", "text", "hello "), flush = false)
        emit(ChatEventDto.PartDelta("ses_test", "msg1", "prt1", "text", "world"))

        assertModel(
            """
            assistant#msg1
            text#prt1:
              hello world
            """,
            m,
        )
    }

    fun `test MessageRemoved removes from SessionModel`() {
        val (m, _, _) = prompted()

        emit(ChatEventDto.MessageUpdated("ses_test", msg("msg1", "ses_test", "user")))
        assertNotNull(m.model.message("msg1"))

        emit(ChatEventDto.MessageRemoved("ses_test", "msg1"))
        assertNull(m.model.message("msg1"))
    }
}
