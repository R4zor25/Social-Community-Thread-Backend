package hu.bme.aut.chat_service.controllers

import hu.bme.aut.chat_service.config.USER_ID_HEADER
import hu.bme.aut.chat_service.domain.ChatConversation
import hu.bme.aut.chat_service.domain.ChatMessage
import hu.bme.aut.chat_service.services.ChatService
import hu.bme.aut.chat_service.services.ForbiddenException
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
        return try {
            val result = chatService.getUsersAllChatMessage(userId)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/{conversationId}")
    fun getChatConversation(@PathVariable conversationId: Long, @PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            val result = chatService.getMessageDetails(userId, conversationId)
            ResponseEntity.ok(result)
        } catch (e: ForbiddenException) {
            ResponseEntity.status(403).body(e.localizedMessage)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/{conversationId}/send")
    fun sendChatMessage(@PathVariable userId: Long, @PathVariable conversationId: Long, @RequestBody chatMessage: ChatMessage) : ResponseEntity<Any> {
        return try {
            val result = chatService.sendMessage(userId, conversationId, chatMessage)
            ResponseEntity.ok(result)
        } catch (e: ForbiddenException) {
            ResponseEntity.status(403).body(e.localizedMessage)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/create")
    fun createChatConversation(@PathVariable userId: Long, @RequestBody chatConversation: ChatConversation): ResponseEntity<Any> {
        return try {
            val result = chatService.createChatConversation(userId, chatConversation)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{conversationId}/addParticipants")
    fun addParticipants(@RequestHeader(USER_ID_HEADER) actingUserId: Long, @PathVariable conversationId: Long, @RequestBody userIds : List<Long>) : ResponseEntity<Any> {
        return try {
            val result = chatService.addParticipants(actingUserId, conversationId, userIds)
            ResponseEntity.ok(result)
        } catch (e: ForbiddenException) {
            ResponseEntity.status(403).body(e.localizedMessage)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{conversationId}/removeParticipants")
    fun removeParticipants(@RequestHeader(USER_ID_HEADER) actingUserId: Long, @PathVariable conversationId: Long, @RequestBody userIds: List<Long>) : ResponseEntity<Any>{
        return try {
            val result = chatService.removeParticipants(actingUserId, conversationId, userIds)
            ResponseEntity.ok(result)
        } catch (e: ForbiddenException) {
            ResponseEntity.status(403).body(e.localizedMessage)
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }
}