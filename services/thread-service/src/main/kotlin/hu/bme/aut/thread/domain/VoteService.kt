package hu.bme.aut.thread.domain

import hu.bme.aut.common.web.error.NotFoundException
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.projection.UserProjections
import hu.bme.aut.thread.persistence.CommentRepository
import hu.bme.aut.thread.persistence.CommentVoteRepository
import hu.bme.aut.thread.persistence.PostRepository
import hu.bme.aut.thread.persistence.PostVoteRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Votes lock the voted row first, so concurrent votes on the same post or comment are applied one after the other:
 * the score changes by the difference between the previous and the new vote, and no update is lost.
 * Setting the same vote again, or clearing a missing one, changes nothing.
 */
@Service
class VoteService(
    private val posts: PostRepository,
    private val postVotes: PostVoteRepository,
    private val comments: CommentRepository,
    private val commentVotes: CommentVoteRepository,
    private val projections: UserProjections
) {
    @Transactional
    fun votePost(caller: CurrentUser, postId: Long, direction: VoteDirection?) {
        if (direction != null) projections.ensure(caller)
        val post = posts.findForUpdate(postId) ?: throw NotFoundException("Post does not exist")
        val key = PostVoteId(postId, caller.id)
        val existing = postVotes.findByIdOrNull(key)
        post.score += voteDelta(existing?.let { VoteDirection.of(it.direction.toInt()) }, direction)
        when {
            direction == null -> existing?.let(postVotes::delete)
            existing == null -> postVotes.save(PostVote(key, direction.value.toShort()))
            else -> existing.direction = direction.value.toShort()
        }
    }

    @Transactional
    fun voteComment(caller: CurrentUser, commentId: Long, direction: VoteDirection?) {
        if (direction != null) projections.ensure(caller)
        val comment = comments.findForUpdate(commentId) ?: throw NotFoundException("Comment does not exist")
        val key = CommentVoteId(commentId, caller.id)
        val existing = commentVotes.findByIdOrNull(key)
        comment.score += voteDelta(existing?.let { VoteDirection.of(it.direction.toInt()) }, direction)
        when {
            direction == null -> existing?.let(commentVotes::delete)
            existing == null -> commentVotes.save(CommentVote(key, direction.value.toShort()))
            else -> existing.direction = direction.value.toShort()
        }
    }
}
