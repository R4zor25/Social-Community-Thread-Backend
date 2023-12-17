package hu.bme.aut.chat_service.services

import hu.bme.aut.chat_service.domain.ChatConversation
import hu.bme.aut.chat_service.domain.ChatMessage

interface ChatService {
    fun getUsersAllChatMessage(userId: Long): List<ChatConversation>
    fun getMessageDetails(userId: Long, conversationId: Long): ChatConversation
    fun sendMessage(userId: Long, chatConversationId: Long, chatMessage: ChatMessage)
    fun createChatConversation(creatorId: Long, chatConversation: ChatConversation)
    fun addParticipants(conversationId: Long, participants : List<Long>)
    fun removeParticipants(conversationId: Long, participants : List<Long>)
}