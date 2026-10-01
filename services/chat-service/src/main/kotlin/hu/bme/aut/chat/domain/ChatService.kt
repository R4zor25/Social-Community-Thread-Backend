package hu.bme.aut.chat.domain

import hu.bme.aut.chat.persistence.ConversationImageRepository
import hu.bme.aut.chat.persistence.ConversationRepository
import hu.bme.aut.chat.persistence.MessageRepository
import hu.bme.aut.chat.persistence.ParticipantRepository
import hu.bme.aut.common.web.error.ApiException
import hu.bme.aut.common.web.error.ForbiddenException
import hu.bme.aut.common.web.error.NotFoundException
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.projection.UserProjections
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** Everything about a conversation is visible to and changeable by its participants only. */
@Service
class ChatService(
    private val conversations: ConversationRepository,
    private val participants: ParticipantRepository,
    private val messages: MessageRepository,
    private val images: ConversationImageRepository,
    private val projections: UserProjections
) {
    private val clock: Clock = Clock.systemUTC()

    @Transactional
    fun create(caller: CurrentUser, name: String, participantIds: Collection<Long>): Conversation {
        projections.ensure(caller)
        val others = participantIds.toSet() - caller.id
        requireKnown(others)
        val now = clock.instant()
        val conversation = conversations.save(Conversation(name, caller.id, now, now))
        (others + caller.id).forEach { participants.add(requireNotNull(conversation.id), it) }
        return conversation
    }

    @Transactional(readOnly = true)
    fun get(caller: CurrentUser, id: Long): Conversation = participantOf(caller, id)

    @Transactional(readOnly = true)
    fun list(caller: CurrentUser, pageable: Pageable): Page<Conversation> = conversations.findAllOf(caller.id, pageable)

    @Transactional
    fun send(caller: CurrentUser, conversationId: Long, body: String): Message {
        val conversation = participantOf(caller, conversationId)
        val message = messages.save(Message(conversationId, caller.id, body, clock.instant()))
        conversation.lastMessageAt = message.sentAt
        return message
    }

    @Transactional(readOnly = true)
    fun messages(caller: CurrentUser, conversationId: Long, pageable: Pageable): Page<Message> {
        participantOf(caller, conversationId)
        return messages.findByConversationIdOrderBySentAtDescIdDesc(conversationId, pageable)
    }

    @Transactional
    fun addParticipants(caller: CurrentUser, conversationId: Long, userIds: Collection<Long>) {
        participantOf(caller, conversationId)
        requireKnown(userIds.toSet())
        userIds.toSet().forEach { participants.add(conversationId, it) }
    }

    /** The conversation is deleted with its last participant. */
    @Transactional
    fun removeParticipant(caller: CurrentUser, conversationId: Long, userId: Long) {
        val conversation = participantOf(caller, conversationId, lock = true)
        if (!mayRemove(caller.id, userId, conversation.creatorId)) throw ForbiddenException("Only the creator can remove other participants")
        val id = ParticipantId(conversationId, userId)
        if (!participants.existsById(id)) return
        participants.deleteById(id)
        participants.flush()
        if (participants.countByIdConversationId(conversationId) == 0L) conversations.delete(conversation)
    }

    @Transactional
    fun setImage(caller: CurrentUser, conversationId: Long, content: ByteArray, rawContentType: String) {
        if (content.size > MAX_IMAGE_BYTES) throw ApiException(HttpStatus.CONTENT_TOO_LARGE, "Images are limited to 5 MB")
        participantOf(caller, conversationId)
        val contentType = MediaType.parseMediaType(rawContentType).let { "${it.type}/${it.subtype}" }
        val image = images.findByIdOrNull(conversationId)
        if (image == null) {
            images.save(ConversationImage(conversationId, content, contentType))
        } else {
            image.content = content
            image.contentType = contentType
        }
    }

    @Transactional(readOnly = true)
    fun image(caller: CurrentUser, conversationId: Long): ConversationImage {
        participantOf(caller, conversationId)
        return images.findByIdOrNull(conversationId) ?: throw NotFoundException("Conversation has no image")
    }

    private fun participantOf(caller: CurrentUser, conversationId: Long, lock: Boolean = false): Conversation {
        val conversation = (if (lock) conversations.findForUpdate(conversationId) else conversations.findByIdOrNull(conversationId)) ?: throw NotFoundException("Conversation does not exist")
        if (!participants.existsById(ParticipantId(conversationId, caller.id))) throw ForbiddenException("You are not a participant of this conversation")
        return conversation
    }

    private fun requireKnown(userIds: Set<Long>) {
        if (projections.usernames(userIds).size != userIds.size) throw NotFoundException("User does not exist")
    }

    companion object {
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    }
}
