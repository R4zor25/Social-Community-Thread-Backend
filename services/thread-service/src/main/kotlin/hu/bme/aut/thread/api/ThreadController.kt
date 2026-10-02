package hu.bme.aut.thread.api

import hu.bme.aut.common.web.paging.PageResponse
import hu.bme.aut.common.web.paging.toBatchResponse
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.common.web.upload.Upload
import hu.bme.aut.thread.domain.PostService
import hu.bme.aut.thread.domain.ThreadService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v2/threads")
class ThreadController(
    private val threads: ThreadService,
    private val posts: PostService,
    private val assembler: ResponseAssembler
) {
    @GetMapping
    fun list(
        caller: CurrentUser,
        @RequestParam(defaultValue = "") query: String,
        @RequestParam(defaultValue = "false") followed: Boolean,
        pageable: Pageable
    ): PageResponse<ThreadResponse> = threads.search(query, caller.id.takeIf { followed }, pageable).toBatchResponse { assembler.threads(it, caller.id) }

    @PostMapping
    fun create(caller: CurrentUser, @Valid @RequestBody request: CreateThreadRequest): ResponseEntity<ThreadResponse> {
        val thread = threads.create(caller, request.name, request.description)
        return ResponseEntity.created(URI("/api/v2/threads/${thread.id}")).body(assembler.threads(listOf(thread), caller.id).single())
    }

    @GetMapping("/{id}")
    fun get(caller: CurrentUser, @PathVariable id: Long): ThreadResponse = assembler.threads(listOf(threads.get(id)), caller.id).single()

    @PatchMapping("/{id}")
    fun update(caller: CurrentUser, @PathVariable id: Long, @Valid @RequestBody request: UpdateThreadRequest): ThreadResponse =
        assembler.threads(listOf(threads.update(caller, id, request.name, request.description)), caller.id).single()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(caller: CurrentUser, @PathVariable id: Long) = threads.delete(caller, id)

    @PutMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun follow(caller: CurrentUser, @PathVariable id: Long) = threads.follow(caller, id)

    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unfollow(caller: CurrentUser, @PathVariable id: Long) = threads.unfollow(caller, id)

    @PutMapping("/{id}/image", consumes = ["image/*"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setImage(caller: CurrentUser, @PathVariable id: Long, upload: Upload) = threads.setImage(caller, id, upload)

    @GetMapping("/{id}/image")
    fun image(@PathVariable id: Long): ResponseEntity<ByteArray> =
        threads.image(id).let { ResponseEntity.ok().contentType(MediaType.parseMediaType(it.contentType)).body(it.content) }

    @GetMapping("/{id}/posts")
    fun posts(caller: CurrentUser, @PathVariable id: Long, @RequestParam(defaultValue = "") query: String, pageable: Pageable): PageResponse<PostResponse> =
        posts.inThread(id, query, pageable).toBatchResponse { assembler.posts(it, caller.id) }

    @PostMapping("/{id}/posts")
    fun createPost(caller: CurrentUser, @PathVariable id: Long, @Valid @RequestBody request: CreatePostRequest): ResponseEntity<PostResponse> {
        val post = posts.create(caller, id, request.title, request.body, request.tags)
        return ResponseEntity.created(URI("/api/v2/posts/${post.id}")).body(assembler.posts(listOf(post), caller.id).single())
    }
}
