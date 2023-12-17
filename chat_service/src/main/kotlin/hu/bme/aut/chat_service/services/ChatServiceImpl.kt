package hu.bme.aut.chat_service.services

import hu.bme.aut.chat_service.domain.AppUser
import hu.bme.aut.chat_service.domain.ChatConversation
import hu.bme.aut.chat_service.domain.ChatMessage
import hu.bme.aut.chat_service.repositories.ChatRepository
import hu.bme.aut.chat_service.repositories.UserRepository
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
        return chatRepository.findById(conversationId).getOrNull() ?: throw EntityNotFoundException("Discussion does not exist!")
    }

    override fun sendMessage(userId: Long, chatConversationId: Long, chatMessage: ChatMessage) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val chatConversation = chatRepository.findById(chatConversationId).getOrNull() ?: throw EntityNotFoundException("Discussion does not exist!")
        chatMessage.apply {
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
            this.creationDate = chatConversation.creationDate
            this.messageList = mutableListOf()
            this.conversationImage = chatConversation.conversationImage
        }
        chatRepository.save(conversation)
    }

    override fun addParticipants(conversationId: Long, participants: List<Long>) {
        val chatConversation = chatRepository.findById(conversationId).getOrNull() ?: throw EntityNotFoundException("Conversation does not exist!")
        val chatParticipants = userRepository.findAllById(participants)
        if(chatParticipants.size != participants.size) {
            throw EntityNotFoundException("User does not exist!")
        }
        chatConversation.chatParticipants += chatParticipants
        chatRepository.save(chatConversation)
    }

    override fun removeParticipants(conversationId: Long, participants: List<Long>) {
        val chatConversation = chatRepository.findById(conversationId).getOrNull() ?: throw EntityNotFoundException("Conversation does not exist!")
        val chatParticipants = userRepository.findAllById(participants)
        if(chatParticipants.size != participants.size) {
            throw EntityNotFoundException("User does not exist!")
        }
        chatConversation.chatParticipants -= userRepository.findAllById(participants).toSet()
        chatRepository.save(chatConversation)
    }
}