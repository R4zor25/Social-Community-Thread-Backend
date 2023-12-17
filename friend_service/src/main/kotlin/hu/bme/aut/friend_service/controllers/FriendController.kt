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
        return try {
            val result = friendService.getUsersAllFriend(userId)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/incoming")
    fun getIncomingFriendRequest(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            val result = friendService.getUsersIncomingFriendRequests(userId)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/outgoing")
    fun getOutgoingFriendRequest(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            val result = friendService.getUsersOutgoingFriendRequests(userId)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/send/{friendId}")
    fun sendFriendRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        return try {
            friendService.sendFriendRequest(userId, friendId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/accept/{friendId}")
    fun acceptRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        return try {
            friendService.acceptFriendRequest(userId, friendId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/decline/{friendId}")
    fun declineRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        return try {
            friendService.declineFriendRequest(userId, friendId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/revoke/{friendId}")
    fun revokeRequest(@PathVariable userId: Long, @PathVariable friendId: Long) : ResponseEntity<Any> {
        return try {
            friendService.revokeRequest(userId, friendId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }


    @DeleteMapping("/{userId}/delete/{friendId}")
    fun deleteFriend(@PathVariable userId: Long, @PathVariable friendId : Long) : ResponseEntity<Any> {
        return try {
            friendService.deleteFriend(userId, friendId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }
}