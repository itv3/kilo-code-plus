package ai.kilocode.client.session.model.message

import ai.kilocode.rpc.dto.MessageDto

data class MessageInfo(
    val id: String,
    val role: String,
    val created: Double,
    val completed: Double? = null,
    val agent: String? = null,
    val providerId: String? = null,
    val modelId: String? = null,
    val parentId: String? = null,
    val cost: Double? = null,
)

/** A single message with its typed contents. */
class Message(
    val info: MessageInfo,
) {
    val parts = LinkedHashMap<String, Content>()
}

fun MessageDto.toInfo() = MessageInfo(
    id = id,
    role = role,
    created = time.created,
    completed = time.completed,
    agent = agent,
    providerId = providerID,
    modelId = modelID,
    parentId = parentID,
    cost = cost,
)
