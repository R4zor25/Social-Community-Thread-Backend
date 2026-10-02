package hu.bme.aut.thread.api

import hu.bme.aut.common.web.paging.PageResponse
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.common.web.upload.Upload
import hu.bme.aut.thread.domain.PostService
import hu.bme.aut.thread.domain.VoteDirection
import hu.bme.aut.thread.domain.VoteService
import jakarta.validation.Valid
import java.net.URI
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/posts")
class PostController(
    private val posts: PostService,
    private val votes: VoteService,
    private val assembler: ResponseAssembler
) {
    @GetMapping
    fun search(
        caller: CurrentUser,
        @RequestParam(required = false) authorId: Long?,
        @RequestParam(defaultValue = "false") saved: Boolean,
        @RequestParam(required = false) voted: VoteDirection?,
        pageable: Pageable
    ): PageResponse<PostResponse> = page(posts.search(caller, authorId, saved, voted, pageable)) { assembler.posts(it, caller.id) }

    @GetMapping("/{id}")
    fun get(caller: CurrentUser, @PathVariable id: Long): PostResponse = assembler.posts(listOf(posts.get(id)), caller.id).single()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(caller: CurrentUser, @PathVariable id: Long) = posts.delete(caller, id)

    @PutMapping("/{id}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun vote(caller: CurrentUser, @PathVariable id: Long, @Valid @RequestBody request: VoteRequest) =
        votes.votePost(caller, id, request.direction)

    @DeleteMapping("/{id}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clearVote(caller: CurrentUser, @PathVariable id: Long) = votes.votePost(caller, id, null)

    @PutMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun save(caller: CurrentUser, @PathVariable id: Long) = posts.save(caller, id)

    @DeleteMapping("/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unsave(caller: CurrentUser, @PathVariable id: Long) = posts.unsave(caller, id)

    @PutMapping("/{id}/attachment", consumes = ["image/*", "video/*"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setAttachment(caller: CurrentUser, @PathVariable id: Long, upload: Upload) = posts.setAttachment(caller, id, upload)

    @GetMapping("/{id}/attachment")
    fun attachment(@PathVariable id: Long): ResponseEntity<ByteArray> =
        posts.attachment(id).let { ResponseEntity.ok().contentType(MediaType.parseMediaType(it.contentType)).body(it.content) }

    @GetMapping("/{id}/comments")
    fun comments(caller: CurrentUser, @PathVariable id: Long, pageable: Pageable): PageResponse<CommentResponse> =
        page(posts.comments(id, pageable)) { assembler.comments(it, caller.id) }

    @PostMapping("/{id}/comments")
    fun comment(caller: CurrentUser, @PathVariable id: Long, @Valid @RequestBody request: CreateCommentRequest): ResponseEntity<CommentResponse> {
        val comment = posts.comment(caller, id, request.body)
        return ResponseEntity.created(URI("/api/v2/posts/$id/comments")).body(assembler.comments(listOf(comment), caller.id).single())
    }
}
