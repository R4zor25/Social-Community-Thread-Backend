package hu.bme.aut.chat.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "conversations")
class Conversation(
    @Column(nullable = false) val name: String,
    @Column(name = "creator_id", nullable = false) val creatorId: Long,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "last_message_at", nullable = false) var lastMessageAt: Instant,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
)

@Entity
@Table(name = "conversation_images")
class ConversationImage(
    @Id @Column(name = "conversation_id") val conversationId: Long,
    @Column(nullable = false) var content: ByteArray,
    @Column(name = "content_type", nullable = false) var contentType: String
)

@Embeddable
data class ParticipantId(
    @Column(name = "conversation_id") val conversationId: Long = 0,
    @Column(name = "user_id") val userId: Long = 0
) : Serializable

@Entity
@Table(name = "conversation_participants")
class Participant(
    @EmbeddedId val id: ParticipantId,
    @Column(name = "joined_at", nullable = false) val joinedAt: Instant
)

@Entity
@Table(name = "messages")
class Message(
    @Column(name = "conversation_id", nullable = false) val conversationId: Long,
    @Column(name = "author_id", nullable = false) val authorId: Long,
    @Column(nullable = false) val body: String,
    @Column(name = "sent_at", nullable = false) val sentAt: Instant,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
)

/** Anyone may leave a conversation; only its creator may remove someone else. */
fun mayRemove(callerId: Long, targetId: Long, creatorId: Long) = callerId == targetId || callerId == creatorId
