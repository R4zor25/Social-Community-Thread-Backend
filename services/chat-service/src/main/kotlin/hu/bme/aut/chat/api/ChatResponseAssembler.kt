package hu.bme.aut.chat.api

import hu.bme.aut.chat.domain.Conversation
import hu.bme.aut.chat.domain.Message
import hu.bme.aut.chat.persistence.ConversationImageRepository
import hu.bme.aut.chat.persistence.ParticipantRepository
import hu.bme.aut.projection.UserProjections
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Builds responses for a whole page at once: one query each for participants, usernames and images, never one per item. */
@Component
class ChatResponseAssembler(
    private val participants: ParticipantRepository,
    private val images: ConversationImageRepository,
    private val projections: UserProjections
) {
    @Transactional(readOnly = true)
    fun conversations(conversations: List<Conversation>): List<ConversationResponse> {
        if (conversations.isEmpty()) return emptyList()
        val ids = conversations.map { requireNotNull(it.id) }
        val members = participants.participantsOf(ids).groupBy({ it[0] as Long }, { it[1] as Long })
        val user = projections.refs(members.values.flatten() + conversations.map { it.creatorId })
        val withImage = images.withImageAmong(ids).toSet()
        return conversations.map {
            ConversationResponse(
                it.id!!, it.name, user(it.creatorId), members[it.id].orEmpty().map(user), it.createdAt, it.lastMessageAt, it.id in withImage
            )
        }
    }

    fun messages(messages: List<Message>): List<MessageResponse> {
        val user = projections.refs(messages.map { it.authorId })
        return messages.map { MessageResponse(it.id!!, it.conversationId, user(it.authorId), it.body, it.sentAt) }
    }
}
