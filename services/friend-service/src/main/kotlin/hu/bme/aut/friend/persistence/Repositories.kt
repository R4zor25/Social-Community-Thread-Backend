package hu.bme.aut.friend.persistence

import hu.bme.aut.friend.domain.FriendRequest
import hu.bme.aut.friend.domain.Friendship
import hu.bme.aut.friend.domain.FriendshipId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FriendRequestRepository : JpaRepository<FriendRequest, Long> {
    fun findByRecipientIdOrderByCreatedAtDescIdDesc(recipientId: Long, pageable: Pageable): Page<FriendRequest>

    fun findBySenderIdOrderByCreatedAtDescIdDesc(senderId: Long, pageable: Pageable): Page<FriendRequest>

    @Query(
        """
        select count(r) > 0 from FriendRequest r
        where (r.senderId = :a and r.recipientId = :b) or (r.senderId = :b and r.recipientId = :a)
        """
    )
    fun existsBetween(a: Long, b: Long): Boolean
}

interface FriendshipRepository : JpaRepository<Friendship, FriendshipId> {
    @Query("select f from Friendship f where f.id.userLow = :userId or f.id.userHigh = :userId order by f.createdAt desc")
    fun findAllOf(userId: Long, pageable: Pageable): Page<Friendship>
}
