package hu.bme.aut.thread.api

import hu.bme.aut.common.web.paging.PageResponse
import hu.bme.aut.common.web.paging.toBatchResponse
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.thread.domain.PostService
import hu.bme.aut.thread.domain.VoteService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class CommentAndFeedController(
    private val posts: PostService,
    private val votes: VoteService,
    private val assembler: ResponseAssembler
) {
    @PutMapping("/api/v2/comments/{id}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun vote(caller: CurrentUser, @PathVariable id: Long, @Valid @RequestBody request: VoteRequest) =
        votes.voteComment(caller, id, request.direction)

    @DeleteMapping("/api/v2/comments/{id}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun clearVote(caller: CurrentUser, @PathVariable id: Long) = votes.voteComment(caller, id, null)

    /** Posts of the threads the caller follows, newest first. */
    @GetMapping("/api/v2/feed")
    fun feed(caller: CurrentUser, pageable: Pageable): PageResponse<PostResponse> =
        posts.feed(caller, pageable).toBatchResponse { assembler.posts(it, caller.id) }
}
