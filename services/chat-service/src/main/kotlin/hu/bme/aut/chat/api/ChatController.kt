package hu.bme.aut.chat.api

import hu.bme.aut.chat.domain.ChatService
import hu.bme.aut.common.web.paging.PageResponse
import hu.bme.aut.common.web.paging.toBatchResponse
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.common.web.upload.Upload
import hu.bme.aut.projection.UserRef
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant

data class ConversationResponse(
    val id: Long,
    val name: String,
    val creator: UserRef,
    val participants: List<UserRef>,
    val createdAt: Instant,
    val lastMessageAt: Instant,
    val hasImage: Boolean
)

data class MessageResponse(val id: Long, val conversationId: Long, val author: UserRef, val body: String, val sentAt: Instant)

data class CreateConversationRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 100) val participantIds: List<Long> = emptyList()
)

data class SendMessageRequest(@field:NotBlank @field:Size(max = 5000) val body: String)

data class AddParticipantsRequest(@field:NotEmpty @field:Size(max = 100) val userIds: List<Long>)

@RestController
@RequestMapping("/api/v2/conversations")
class ChatController(private val chat: ChatService, private val assembler: ChatResponseAssembler) {
    @GetMapping
    fun list(caller: CurrentUser, pageable: Pageable): PageResponse<ConversationResponse> = chat.list(caller, pageable).toBatchResponse(assembler::conversations)

    @PostMapping
    fun create(caller: CurrentUser, @Valid @RequestBody request: CreateConversationRequest): ResponseEntity<ConversationResponse> {
        val conversation = chat.create(caller, request.name, request.participantIds)
        return ResponseEntity.created(URI("/api/v2/conversations/${conversation.id}")).body(assembler.conversations(listOf(conversation)).single())
    }

    @GetMapping("/{id}")
    fun get(caller: CurrentUser, @PathVariable id: Long): ConversationResponse = assembler.conversations(listOf(chat.get(caller, id))).single()

    @GetMapping("/{id}/messages")
    fun messages(caller: CurrentUser, @PathVariable id: Long, pageable: Pageable): PageResponse<MessageResponse> =
        chat.messages(caller, id, pageable).toBatchResponse(assembler::messages)

    @PostMapping("/{id}/messages")
    fun send(caller: CurrentUser, @PathVariable id: Long, @Valid @RequestBody request: SendMessageRequest): ResponseEntity<MessageResponse> {
        val message = chat.send(caller, id, request.body)
        return ResponseEntity.created(URI("/api/v2/conversations/$id/messages")).body(assembler.messages(listOf(message)).single())
    }

    @PostMapping("/{id}/participants")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addParticipants(caller: CurrentUser, @PathVariable id: Long, @Valid @RequestBody request: AddParticipantsRequest) =
        chat.addParticipants(caller, id, request.userIds)

    @DeleteMapping("/{id}/participants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeParticipant(caller: CurrentUser, @PathVariable id: Long, @PathVariable userId: Long) = chat.removeParticipant(caller, id, userId)

    @PutMapping("/{id}/image", consumes = ["image/*"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setImage(caller: CurrentUser, @PathVariable id: Long, upload: Upload) = chat.setImage(caller, id, upload)

    @GetMapping("/{id}/image")
    fun image(caller: CurrentUser, @PathVariable id: Long): ResponseEntity<ByteArray> =
        chat.image(caller, id).let { ResponseEntity.ok().contentType(MediaType.parseMediaType(it.contentType)).body(it.content) }
}
