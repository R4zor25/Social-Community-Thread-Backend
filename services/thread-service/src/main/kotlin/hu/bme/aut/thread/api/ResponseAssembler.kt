package hu.bme.aut.thread.api

import hu.bme.aut.projection.UserProjections
import hu.bme.aut.thread.domain.Comment
import hu.bme.aut.thread.domain.Post
import hu.bme.aut.thread.domain.TopicThread
import hu.bme.aut.thread.domain.VoteDirection
import hu.bme.aut.thread.persistence.CommentRepository
import hu.bme.aut.thread.persistence.CommentVoteRepository
import hu.bme.aut.thread.persistence.FollowAndSaveRepository
import hu.bme.aut.thread.persistence.PostAttachmentRepository
import hu.bme.aut.thread.persistence.PostVoteRepository
import hu.bme.aut.thread.persistence.ThreadImageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Builds responses for a whole page at once: a fixed number of queries per page (usernames, the viewer's votes,
 * saves, follows, attachments, comment counts), never one per item.
 */
@Component
class ResponseAssembler(
    private val projections: UserProjections,
    private val followAndSave: FollowAndSaveRepository,
    private val images: ThreadImageRepository,
    private val attachments: PostAttachmentRepository,
    private val postVotes: PostVoteRepository,
    private val comments: CommentRepository,
    private val commentVotes: CommentVoteRepository
) {
    @Transactional(readOnly = true)
    fun threads(threads: List<TopicThread>, viewerId: Long): List<ThreadResponse> {
        if (threads.isEmpty()) return emptyList()
        val ids = threads.map { requireNotNull(it.id) }
        val names = projections.usernames(threads.map { it.creatorId })
        val followed = followAndSave.followedAmong(viewerId, ids).toSet()
        val withImage = images.withImageAmong(ids).toSet()
        return threads.map {
            ThreadResponse(
                it.id!!, it.name, it.description, user(it.creatorId, names), it.createdAt,
                followedByMe = it.id in followed, hasImage = it.id in withImage
            )
        }
    }

    @Transactional(readOnly = true)
    fun posts(posts: List<Post>, viewerId: Long): List<PostResponse> {
        if (posts.isEmpty()) return emptyList()
        val ids = posts.map { requireNotNull(it.id) }
        val names = projections.usernames(posts.map { it.authorId })
        val votes = postVotes.byUserAmong(viewerId, ids).associate { it.id.postId to VoteDirection.of(it.direction.toInt()) }
        val saved = followAndSave.savedAmong(viewerId, ids).toSet()
        val attachmentTypes = attachments.typesAmong(ids).associate { (it[0] as Long) to (it[1] as String) }
        val commentCounts = comments.countsAmong(ids).associate { (it[0] as Long) to (it[1] as Long) }
        return posts.map {
            PostResponse(
                it.id!!, it.threadId, user(it.authorId, names), it.title, it.body, it.tags, it.score,
                myVote = votes[it.id], savedByMe = it.id in saved, attachmentType = attachmentTypes[it.id],
                commentCount = commentCounts[it.id] ?: 0, createdAt = it.createdAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun comments(comments: List<Comment>, viewerId: Long): List<CommentResponse> {
        if (comments.isEmpty()) return emptyList()
        val names = projections.usernames(comments.map { it.authorId })
        val votes = commentVotes.byUserAmong(viewerId, comments.map { requireNotNull(it.id) })
            .associate { it.id.commentId to VoteDirection.of(it.direction.toInt()) }
        return comments.map {
            CommentResponse(it.id!!, it.postId, user(it.authorId, names), it.body, it.score, votes[it.id], it.createdAt)
        }
    }

    private fun user(id: Long, names: Map<Long, String>) = UserRef(id, names[id] ?: "unknown")
}
