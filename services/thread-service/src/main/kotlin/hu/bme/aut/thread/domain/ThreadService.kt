package hu.bme.aut.thread.domain

import hu.bme.aut.common.web.error.ForbiddenException
import hu.bme.aut.common.web.error.NotFoundException
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.common.web.upload.Upload
import hu.bme.aut.projection.UserProjections
import hu.bme.aut.thread.persistence.FollowAndSaveRepository
import hu.bme.aut.thread.persistence.ThreadImageRepository
import hu.bme.aut.thread.persistence.ThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class ThreadService(
    private val threads: ThreadRepository,
    private val images: ThreadImageRepository,
    private val followers: FollowAndSaveRepository,
    private val projections: UserProjections
) {
    private val clock: Clock = Clock.systemUTC()

    @Transactional
    fun create(caller: CurrentUser, name: String, description: String): TopicThread {
        projections.ensure(caller)
        return threads.save(TopicThread(name, description, caller.id, clock.instant()))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): TopicThread = threads.findByIdOrNull(id) ?: throw NotFoundException("Thread does not exist")

    @Transactional(readOnly = true)
    fun search(query: String, followedBy: Long?, pageable: Pageable): Page<TopicThread> = threads.search(query, followedBy, pageable)

    @Transactional
    fun update(caller: CurrentUser, id: Long, name: String?, description: String?): TopicThread {
        val thread = ownedBy(caller, id)
        name?.let { thread.name = it }
        description?.let { thread.description = it }
        return thread
    }

    /** Posts, comments, votes, saves and followers go with the thread (ON DELETE CASCADE); users never do. */
    @Transactional
    fun delete(caller: CurrentUser, id: Long) = threads.delete(ownedBy(caller, id))

    @Transactional
    fun follow(caller: CurrentUser, id: Long) {
        get(id)
        projections.ensure(caller)
        followers.follow(id, caller.id)
    }

    @Transactional
    fun unfollow(caller: CurrentUser, id: Long) {
        get(id)
        followers.unfollow(id, caller.id)
    }

    @Transactional
    fun setImage(caller: CurrentUser, id: Long, upload: Upload) {
        ownedBy(caller, id)
        val image = images.findByIdOrNull(id)
        if (image == null) {
            images.save(ThreadImage(id, upload.content, upload.contentType))
        } else {
            image.content = upload.content
            image.contentType = upload.contentType
        }
    }

    @Transactional(readOnly = true)
    fun image(id: Long): ThreadImage {
        get(id)
        return images.findByIdOrNull(id) ?: throw NotFoundException("Thread has no image")
    }

    private fun ownedBy(caller: CurrentUser, id: Long): TopicThread {
        val thread = get(id)
        if (thread.creatorId != caller.id) throw ForbiddenException("Only the creator can change this thread")
        return thread
    }
}
