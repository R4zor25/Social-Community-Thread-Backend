package hu.bme.aut.friend.domain

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
@Table(name = "friend_requests")
class FriendRequest(
    @Column(name = "sender_id", nullable = false) val senderId: Long,
    @Column(name = "recipient_id", nullable = false) val recipientId: Long,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
)

/** A friendship is stored once per pair, with the lower user id first. */
@Embeddable
data class FriendshipId(
    @Column(name = "user_low") val userLow: Long = 0,
    @Column(name = "user_high") val userHigh: Long = 0
) : Serializable {
    fun other(userId: Long) = if (userId == userLow) userHigh else userLow

    companion object {
        fun of(a: Long, b: Long) = FriendshipId(minOf(a, b), maxOf(a, b))
    }
}

@Entity
@Table(name = "friendships")
class Friendship(
    @EmbeddedId val id: FriendshipId,
    @Column(name = "created_at", nullable = false) val createdAt: Instant
)
