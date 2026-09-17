package hu.bme.aut.friend_service.controllers

import hu.bme.aut.friend_service.services.FriendService
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
@Slf4j
class FriendController(
    private var friendService: FriendService
) {

    @GetMapping("/{userId}")
    fun getAllFriends(@PathVariable userId: Long): ResponseEntity<Any> {
        val result = friendService.getUsersAllFriend(userId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{userId}/incoming")
    fun getIncomingFriendRequest(@PathVariable userId: Long): ResponseEntity<Any> {
        val result = friendService.getUsersIncomingFriendRequests(userId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{userId}/outgoing")
    fun getOutgoingFriendRequest(@PathVariable userId: Long): ResponseEntity<Any> {
        val result = friendService.getUsersOutgoingFriendRequests(userId)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{userId}/send/{friendId}")
    fun sendFriendRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        friendService.sendFriendRequest(userId, friendId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/accept/{friendId}")
    fun acceptRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        friendService.acceptFriendRequest(userId, friendId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/decline/{friendId}")
    fun declineRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        friendService.declineFriendRequest(userId, friendId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/revoke/{friendId}")
    fun revokeRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        friendService.revokeRequest(userId, friendId)
        return ResponseEntity.ok().build()
    }


    @DeleteMapping("/{userId}/delete/{friendId}")
    fun deleteFriend(@PathVariable userId: Long, @PathVariable friendId : Long) : ResponseEntity<Any> {
        friendService.deleteFriend(userId, friendId)
        return ResponseEntity.ok().build()
    }
}