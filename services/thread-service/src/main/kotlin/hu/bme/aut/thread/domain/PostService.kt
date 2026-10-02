package hu.bme.aut.thread.domain

import hu.bme.aut.common.web.error.ForbiddenException
import hu.bme.aut.common.web.error.NotFoundException
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.common.web.upload.Upload
import hu.bme.aut.projection.UserProjections
import hu.bme.aut.thread.persistence.CommentRepository
import hu.bme.aut.thread.persistence.FollowAndSaveRepository
import hu.bme.aut.thread.persistence.PostAttachmentRepository
import hu.bme.aut.thread.persistence.PostRepository
import hu.bme.aut.thread.persistence.ThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class PostService(
    private val threads: ThreadRepository,
    private val posts: PostRepository,
    private val attachments: PostAttachmentRepository,
    private val comments: CommentRepository,
    private val saves: FollowAndSaveRepository,
    private val projections: UserProjections
) {
    private val clock: Clock = Clock.systemUTC()

    /** Ids, author, score and time are set here; the request only supplies the content. */
    @Transactional
    fun create(caller: CurrentUser, threadId: Long, title: String, body: String, tags: List<String>): Post {
        if (!threads.existsById(threadId)) throw NotFoundException("Thread does not exist")
        projections.ensure(caller)
        return posts.save(Post(threadId, caller.id, title, body, tags, clock.instant()))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Post = posts.findByIdOrNull(id) ?: throw NotFoundException("Post does not exist")

    @Transactional(readOnly = true)
    fun inThread(threadId: Long, query: String, pageable: Pageable): Page<Post> {
        if (!threads.existsById(threadId)) throw NotFoundException("Thread does not exist")
        return posts.inThread(threadId, query, pageable)
    }

    @Transactional(readOnly = true)
    fun search(caller: CurrentUser, authorId: Long?, saved: Boolean, voted: VoteDirection?, pageable: Pageable): Page<Post> =
        posts.search(authorId, caller.id.takeIf { saved }, caller.id.takeIf { voted != null }, voted?.value?.toShort(), pageable)

    @Transactional(readOnly = true)
    fun feed(caller: CurrentUser, pageable: Pageable): Page<Post> = posts.feed(caller.id, pageable)

    /** Comments, votes, saves and the attachment go with the post (ON DELETE CASCADE); nothing else does. */
    @Transactional
    fun delete(caller: CurrentUser, id: Long) = posts.delete(authoredBy(caller, id))

    @Transactional
    fun save(caller: CurrentUser, id: Long) {
        get(id)
        projections.ensure(caller)
        saves.save(caller.id, id)
    }

    @Transactional
    fun unsave(caller: CurrentUser, id: Long) {
        get(id)
        saves.unsave(caller.id, id)
    }

    @Transactional
    fun setAttachment(caller: CurrentUser, id: Long, upload: Upload) {
        authoredBy(caller, id)
        val attachment = attachments.findByIdOrNull(id)
        if (attachment == null) {
            attachments.save(PostAttachment(id, upload.content, upload.contentType))
        } else {
            attachment.content = upload.content
            attachment.contentType = upload.contentType
        }
    }

    @Transactional(readOnly = true)
    fun attachment(id: Long): PostAttachment {
        get(id)
        return attachments.findByIdOrNull(id) ?: throw NotFoundException("Post has no attachment")
    }

    @Transactional
    fun comment(caller: CurrentUser, postId: Long, body: String): Comment {
        get(postId)
        projections.ensure(caller)
        return comments.save(Comment(postId, caller.id, body, clock.instant()))
    }

    @Transactional(readOnly = true)
    fun comments(postId: Long, pageable: Pageable): Page<Comment> {
        get(postId)
        return comments.findByPostIdOrderByCreatedAtAscIdAsc(postId, pageable)
    }

    private fun authoredBy(caller: CurrentUser, id: Long): Post {
        val post = get(id)
        if (post.authorId != caller.id) throw ForbiddenException("Only the author can delete this post")
        return post
    }
}
