package hu.bme.aut.chat_service.controllers

import hu.bme.aut.common.identity.USER_ID_HEADER
import hu.bme.aut.chat_service.domain.ChatConversation
import hu.bme.aut.chat_service.domain.ChatMessage
import hu.bme.aut.chat_service.services.ChatService
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
class ChatController(
    private var chatService: ChatService
) {

    @GetMapping("/{userId}/conversations")
    fun getAllChatConversationForUser(@PathVariable userId: Long): ResponseEntity<Any> {
        val result = chatService.getUsersAllChatMessage(userId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{userId}/{conversationId}")
    fun getChatConversation(@PathVariable conversationId: Long, @PathVariable userId: Long): ResponseEntity<Any> {
        val result = chatService.getMessageDetails(userId, conversationId)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{userId}/{conversationId}/send")
    fun sendChatMessage(@PathVariable userId: Long, @PathVariable conversationId: Long, @RequestBody chatMessage: ChatMessage) : ResponseEntity<Any> {
        val result = chatService.sendMessage(userId, conversationId, chatMessage)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{userId}/create")
    fun createChatConversation(@PathVariable userId: Long, @RequestBody chatConversation: ChatConversation): ResponseEntity<Any> {
        val result = chatService.createChatConversation(userId, chatConversation)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{conversationId}/addParticipants")
    fun addParticipants(@RequestHeader(USER_ID_HEADER) actingUserId: Long, @PathVariable conversationId: Long, @RequestBody userIds : List<Long>) : ResponseEntity<Any> {
        val result = chatService.addParticipants(actingUserId, conversationId, userIds)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{conversationId}/removeParticipants")
    fun removeParticipants(@RequestHeader(USER_ID_HEADER) actingUserId: Long, @PathVariable conversationId: Long, @RequestBody userIds: List<Long>) : ResponseEntity<Any>{
        val result = chatService.removeParticipants(actingUserId, conversationId, userIds)
        return ResponseEntity.ok(result)
    }
}