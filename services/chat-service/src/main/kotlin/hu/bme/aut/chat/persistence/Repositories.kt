package hu.bme.aut.chat.persistence

import hu.bme.aut.chat.domain.Conversation
import hu.bme.aut.chat.domain.ConversationImage
import hu.bme.aut.chat.domain.Message
import hu.bme.aut.chat.domain.Participant
import hu.bme.aut.chat.domain.ParticipantId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ConversationRepository : JpaRepository<Conversation, Long> {
    /** Participant removals are serialized, so exactly one of them sees the conversation become empty. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    fun findForUpdate(id: Long): Conversation?

    @Query(
        """
        select c from Conversation c
        where exists (select 1 from Participant p where p.id.conversationId = c.id and p.id.userId = :userId)
        order by c.lastMessageAt desc, c.id desc
        """
    )
    fun findAllOf(userId: Long, pageable: Pageable): Page<Conversation>
}

interface ConversationImageRepository : JpaRepository<ConversationImage, Long> {
    @Query("select i.conversationId from ConversationImage i where i.conversationId in :ids")
    fun withImageAmong(ids: Collection<Long>): List<Long>
}

interface ParticipantRepository : JpaRepository<Participant, ParticipantId> {
    @Modifying
    @Query(nativeQuery = true, value = "insert into conversation_participants (conversation_id, user_id, joined_at) values (:conversationId, :userId, now()) on conflict do nothing")
    fun add(conversationId: Long, userId: Long)

    fun countByIdConversationId(conversationId: Long): Long

    @Query("select p.id.conversationId, p.id.userId from Participant p where p.id.conversationId in :ids order by p.joinedAt, p.id.userId")
    fun participantsOf(ids: Collection<Long>): List<Array<Any>>
}

interface MessageRepository : JpaRepository<Message, Long> {
    fun findByConversationIdOrderBySentAtDescIdDesc(conversationId: Long, pageable: Pageable): Page<Message>
}
