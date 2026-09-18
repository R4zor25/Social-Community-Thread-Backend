package hu.bme.aut.chat_service.services

import hu.bme.aut.chat_service.domain.AppUser
import hu.bme.aut.chat_service.domain.ChatConversation
import hu.bme.aut.chat_service.domain.ChatMessage
import hu.bme.aut.chat_service.repositories.ChatRepository
import hu.bme.aut.chat_service.repositories.UserRepository
import hu.bme.aut.common.error.ForbiddenException
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Service
import java.util.*
import kotlin.jvm.optionals.getOrNull

@OptIn(ExperimentalStdlibApi::class)
@Service
@RequiredArgsConstructor
@Transactional
class ChatServiceImpl(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository
) : ChatService {

    override fun getUsersAllChatMessage(userId: Long): List<ChatConversation> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        return chatRepository.findAll().filter { it.chatParticipants.contains(user) }
    }

    override fun getMessageDetails(userId: Long, conversationId: Long): ChatConversation {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val chatConversation = chatRepository.findById(conversationId).getOrNull() ?: throw EntityNotFoundException("Discussion does not exist!")
        requireParticipant(chatConversation, userId)
        return chatConversation
    }

    override fun sendMessage(userId: Long, chatConversationId: Long, chatMessage: ChatMessage) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val chatConversation = chatRepository.findById(chatConversationId).getOrNull() ?: throw EntityNotFoundException("Discussion does not exist!")
        requireParticipant(chatConversation, userId)
        chatMessage.apply {
            this.id = null
            this.author = user
            this.sentDate = Date()
        }
        chatConversation.messageList.add(chatMessage)
        chatConversation.lastMessageDate = Date()
        chatRepository.save(chatConversation)
    }

    override fun createChatConversation(creatorId: Long,  chatConversation: ChatConversation) {
        val chatCreator = userRepository.findById(creatorId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val chatParticipants : MutableList<AppUser> = mutableListOf()
        chatParticipants += chatCreator
        val conversation = ChatConversation().apply {
            this.chatCreator = chatCreator
            this.chatParticipants = chatParticipants
            this.conversationName = chatConversation.conversationName
            this.creationDate = Date()
            this.messageList = mutableListOf()
            this.conversationImage = chatConversation.conversationImage
        }
        chatRepository.save(conversation)
    }

    override fun addParticipants(actingUserId: Long, conversationId: Long, participants: List<Long>) {
        val chatConversation = chatRepository.findById(conversationId).getOrNull() ?: throw EntityNotFoundException("Conversation does not exist!")
        requireParticipant(chatConversation, actingUserId)
        val chatParticipants = userRepository.findAllById(participants)
        if(chatParticipants.size != participants.size) {
            throw EntityNotFoundException("User does not exist!")
        }
        chatConversation.chatParticipants += chatParticipants
        chatRepository.save(chatConversation)
    }

    override fun removeParticipants(actingUserId: Long, conversationId: Long, participants: List<Long>) {
        val chatConversation = chatRepository.findById(conversationId).getOrNull() ?: throw EntityNotFoundException("Conversation does not exist!")
        requireParticipant(chatConversation, actingUserId)
        val chatParticipants = userRepository.findAllById(participants)
        if(chatParticipants.size != participants.size) {
            throw EntityNotFoundException("User does not exist!")
        }
        chatConversation.chatParticipants -= userRepository.findAllById(participants).toSet()
        chatRepository.save(chatConversation)
    }

    private fun requireParticipant(chatConversation: ChatConversation, userId: Long) {
        if (chatConversation.chatParticipants.none { it.userId == userId }) {
            throw ForbiddenException("User is not a participant of this conversation!")
        }
    }
}
