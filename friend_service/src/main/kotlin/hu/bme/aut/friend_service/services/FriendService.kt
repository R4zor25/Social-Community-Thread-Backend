package hu.bme.aut.friend_service.services

import hu.bme.aut.friend_service.domain.AppUser

interface FriendService {
    fun getUsersAllFriend(userId: Long) : List<AppUser>
    fun getUsersIncomingFriendRequests(userId: Long) : List<AppUser>
    fun getUsersOutgoingFriendRequests(userId: Long): List<AppUser>
    fun acceptFriendRequest(userId: Long, friendId: Long)
    fun sendFriendRequest(userId: Long, friendId: Long)
    fun declineFriendRequest(userId: Long, friendId: Long)
    fun revokeRequest(userId: Long, friendId: Long)
    fun deleteFriend(userId: Long, friendId: Long)
}