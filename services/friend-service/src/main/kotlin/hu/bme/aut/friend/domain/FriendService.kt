package hu.bme.aut.friend.domain

import hu.bme.aut.common.web.error.ApiException
import hu.bme.aut.common.web.error.ConflictException
import hu.bme.aut.common.web.error.ForbiddenException
import hu.bme.aut.common.web.error.NotFoundException
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.friend.persistence.FriendRequestRepository
import hu.bme.aut.friend.persistence.FriendshipRepository
import hu.bme.aut.projection.UserProjections
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

enum class RequestDirection { INCOMING, OUTGOING }

@Service
class FriendService(
    private val requests: FriendRequestRepository,
    private val friendships: FriendshipRepository,
    private val projections: UserProjections
) {
    private val clock: Clock = Clock.systemUTC()

    /**
     * The recipient must be known from user-events. The unique pair index rejects a request that races with the
     * checks below (e.g. both users requesting each other at once); that surfaces as 409 as well.
     */
    @Transactional
    fun send(caller: CurrentUser, recipientId: Long): FriendRequest {
        if (recipientId == caller.id) throw ApiException(HttpStatus.BAD_REQUEST, "You cannot send a friend request to yourself")
        if (!projections.exists(recipientId)) throw NotFoundException("User does not exist")
        projections.ensure(caller)
        if (friendships.existsById(FriendshipId.of(caller.id, recipientId))) throw ConflictException("You are already friends")
        if (requests.existsBetween(caller.id, recipientId)) throw ConflictException("A friend request between you already exists")
        return requests.saveAndFlush(FriendRequest(caller.id, recipientId, clock.instant()))
    }

    @Transactional
    fun accept(caller: CurrentUser, requestId: Long) {
        val request = addressedTo(caller, requestId)
        requests.delete(request)
        friendships.save(Friendship(FriendshipId.of(request.senderId, request.recipientId), clock.instant()))
    }

    @Transactional
    fun decline(caller: CurrentUser, requestId: Long) = requests.delete(addressedTo(caller, requestId))

    @Transactional
    fun revoke(caller: CurrentUser, requestId: Long) {
        val request = find(requestId)
        if (request.senderId != caller.id) throw ForbiddenException("Only the sender can revoke this request")
        requests.delete(request)
    }

    @Transactional(readOnly = true)
    fun requests(caller: CurrentUser, direction: RequestDirection, pageable: Pageable): Page<FriendRequest> = when (direction) {
        RequestDirection.INCOMING -> requests.findByRecipientIdOrderByCreatedAtDescIdDesc(caller.id, pageable)
        RequestDirection.OUTGOING -> requests.findBySenderIdOrderByCreatedAtDescIdDesc(caller.id, pageable)
    }

    @Transactional(readOnly = true)
    fun friends(caller: CurrentUser, pageable: Pageable): Page<Friendship> = friendships.findAllOf(caller.id, pageable)

    @Transactional
    fun unfriend(caller: CurrentUser, userId: Long) {
        val id = FriendshipId.of(caller.id, userId)
        if (friendships.existsById(id)) friendships.deleteById(id)
    }

    private fun find(requestId: Long) = requests.findByIdOrNull(requestId) ?: throw NotFoundException("Friend request does not exist")

    private fun addressedTo(caller: CurrentUser, requestId: Long): FriendRequest {
        val request = find(requestId)
        if (request.recipientId != caller.id) throw ForbiddenException("Only the recipient can answer this request")
        return request
    }
}
