package hu.bme.aut.friend_service.services

import hu.bme.aut.friend_service.domain.AppUser
import hu.bme.aut.friend_service.repositories.UserRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
@RequiredArgsConstructor
@Transactional
class FriendServiceImpl(
    private val userRepository: UserRepository
) : FriendService {

    override fun getUsersAllFriend(userId: Long): List<AppUser> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        return user.friends.toList()
    }

    override fun getUsersIncomingFriendRequests(userId: Long): List<AppUser> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        return user.incomingFriendRequests.toList()
    }

    override fun getUsersOutgoingFriendRequests(userId: Long): List<AppUser> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        return user.outgoingFriendRequests.toList()
    }

    override fun acceptFriendRequest(userId: Long, friendId: Long){
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val friend = userRepository.findById(friendId).getOrNull() ?: throw EntityNotFoundException("Friend does not exist!")
        user.incomingFriendRequests.remove(friend)
        friend.outgoingFriendRequests.remove(user)
        user.friends.add(friend)
        friend.friends.add(user)
        userRepository.saveAll(listOf(user, friend))
    }

    override fun sendFriendRequest(userId: Long, friendId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val friend = userRepository.findById(friendId).getOrNull() ?: throw EntityNotFoundException("Friend does not exist!")
        user.outgoingFriendRequests.add(friend)
        friend.incomingFriendRequests.add(user)
        userRepository.saveAll(listOf(user, friend))
    }

    override fun declineFriendRequest(userId: Long, friendId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val friend = userRepository.findById(friendId).getOrNull() ?: throw EntityNotFoundException("Friend does not exist!")
        user.incomingFriendRequests.remove(friend)
        friend.outgoingFriendRequests.remove(user)
        userRepository.saveAll(listOf(user, friend))
    }

    override fun revokeRequest(userId: Long, friendId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val friend = userRepository.findById(friendId).getOrNull() ?: throw EntityNotFoundException("Friend does not exist!")
        user.outgoingFriendRequests.remove(friend)
        friend.incomingFriendRequests.remove(user)
        userRepository.saveAll(listOf(user, friend))
    }

    override fun deleteFriend(userId: Long, friendId: Long){
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val friend = userRepository.findById(friendId).getOrNull() ?: throw EntityNotFoundException("Friend does not exist!")
        user.friends.remove(friend)
        friend.friends.remove(user)
        userRepository.saveAll(listOf(user, friend))
    }
}