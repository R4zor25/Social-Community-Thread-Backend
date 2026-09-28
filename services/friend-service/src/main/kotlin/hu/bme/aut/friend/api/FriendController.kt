package hu.bme.aut.friend.api

import hu.bme.aut.common.web.error.ApiException
import hu.bme.aut.common.web.paging.PageResponse
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.friend.domain.FriendRequest
import hu.bme.aut.friend.domain.FriendService
import hu.bme.aut.friend.domain.RequestDirection
import hu.bme.aut.projection.UserProjections
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant

data class UserRef(val id: Long, val username: String)
data class FriendResponse(val user: UserRef, val since: Instant)
data class FriendRequestResponse(val id: Long, val sender: UserRef, val recipient: UserRef, val createdAt: Instant)
data class SendFriendRequest(@field:NotNull val recipientId: Long?)

@RestController
class FriendController(private val friends: FriendService, private val projections: UserProjections) {

    @GetMapping("/api/v2/friends")
    fun friends(caller: CurrentUser, pageable: Pageable): PageResponse<FriendResponse> = page(friends.friends(caller, pageable)) { page ->
        val others = page.associateWith { it.id.other(caller.id) }
        val names = projections.usernames(others.values)
        page.map { FriendResponse(UserRef(others.getValue(it), names[others.getValue(it)] ?: "unknown"), it.createdAt) }
    }

    @DeleteMapping("/api/v2/friends/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unfriend(caller: CurrentUser, @PathVariable userId: Long) = friends.unfriend(caller, userId)

    @GetMapping("/api/v2/friend-requests")
    fun requests(caller: CurrentUser, @RequestParam(defaultValue = "incoming") direction: String, pageable: Pageable): PageResponse<FriendRequestResponse> {
        val parsed = RequestDirection.entries.firstOrNull { it.name.equals(direction, ignoreCase = true) }
            ?: throw ApiException(HttpStatus.BAD_REQUEST, "direction must be incoming or outgoing")
        return page(friends.requests(caller, parsed, pageable), ::toResponses)
    }

    @PostMapping("/api/v2/friend-requests")
    fun send(caller: CurrentUser, @Valid @RequestBody request: SendFriendRequest): ResponseEntity<FriendRequestResponse> {
        val created = friends.send(caller, requireNotNull(request.recipientId))
        return ResponseEntity.created(URI("/api/v2/friend-requests/${created.id}")).body(toResponses(listOf(created)).single())
    }

    @PostMapping("/api/v2/friend-requests/{id}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun accept(caller: CurrentUser, @PathVariable id: Long) = friends.accept(caller, id)

    @PostMapping("/api/v2/friend-requests/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun decline(caller: CurrentUser, @PathVariable id: Long) = friends.decline(caller, id)

    @DeleteMapping("/api/v2/friend-requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(caller: CurrentUser, @PathVariable id: Long) = friends.revoke(caller, id)

    private fun toResponses(requests: List<FriendRequest>): List<FriendRequestResponse> {
        val names = projections.usernames(requests.flatMap { listOf(it.senderId, it.recipientId) })
        fun ref(id: Long) = UserRef(id, names[id] ?: "unknown")
        return requests.map { FriendRequestResponse(it.id!!, ref(it.senderId), ref(it.recipientId), it.createdAt) }
    }

    private fun <T : Any, R> page(page: Page<T>, mapper: (List<T>) -> List<R>) =
        PageResponse(mapper(page.content), page.number, page.size, page.totalElements, page.totalPages)
}
