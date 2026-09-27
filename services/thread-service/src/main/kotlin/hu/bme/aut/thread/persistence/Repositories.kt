package hu.bme.aut.thread.persistence

import hu.bme.aut.thread.domain.Comment
import hu.bme.aut.thread.domain.CommentVote
import hu.bme.aut.thread.domain.CommentVoteId
import hu.bme.aut.thread.domain.Post
import hu.bme.aut.thread.domain.PostAttachment
import hu.bme.aut.thread.domain.PostVote
import hu.bme.aut.thread.domain.PostVoteId
import hu.bme.aut.thread.domain.ThreadImage
import hu.bme.aut.thread.domain.TopicThread
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ThreadRepository : JpaRepository<TopicThread, Long> {
    @Query(
        """
        select t from TopicThread t
        where lower(t.name) like lower(concat('%', :query, '%'))
          and (:followerId is null or exists (
              select 1 from ThreadFollower f where f.id.threadId = t.id and f.id.userId = :followerId))
        order by t.createdAt desc, t.id desc
        """
    )
    fun search(query: String, followerId: Long?, pageable: Pageable): Page<TopicThread>
}

interface ThreadImageRepository : JpaRepository<ThreadImage, Long> {
    @Query("select i.threadId from ThreadImage i where i.threadId in :threadIds")
    fun withImageAmong(threadIds: Collection<Long>): List<Long>
}

/** Plain SQL for follow and save, so that repeating them is a no-op even under concurrent requests. */
interface FollowAndSaveRepository : JpaRepository<TopicThread, Long> {
    @Modifying
    @Query(nativeQuery = true, value = "insert into thread_followers (thread_id, user_id, followed_at) values (:threadId, :userId, now()) on conflict do nothing")
    fun follow(threadId: Long, userId: Long)

    @Modifying
    @Query(nativeQuery = true, value = "delete from thread_followers where thread_id = :threadId and user_id = :userId")
    fun unfollow(threadId: Long, userId: Long)

    @Query(nativeQuery = true, value = "select thread_id from thread_followers where user_id = :userId and thread_id in (:threadIds)")
    fun followedAmong(userId: Long, threadIds: Collection<Long>): List<Long>

    @Modifying
    @Query(nativeQuery = true, value = "insert into saved_posts (user_id, post_id, saved_at) values (:userId, :postId, now()) on conflict do nothing")
    fun save(userId: Long, postId: Long)

    @Modifying
    @Query(nativeQuery = true, value = "delete from saved_posts where user_id = :userId and post_id = :postId")
    fun unsave(userId: Long, postId: Long)

    @Query(nativeQuery = true, value = "select post_id from saved_posts where user_id = :userId and post_id in (:postIds)")
    fun savedAmong(userId: Long, postIds: Collection<Long>): List<Long>
}

interface PostRepository : JpaRepository<Post, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    fun findForUpdate(id: Long): Post?

    @Query(
        """
        select p from Post p
        where p.threadId = :threadId and lower(p.title) like lower(concat('%', :query, '%'))
        order by p.createdAt desc, p.id desc
        """
    )
    fun inThread(threadId: Long, query: String, pageable: Pageable): Page<Post>

    @Query(
        """
        select p from Post p
        where (:authorId is null or p.authorId = :authorId)
          and (:savedBy is null or exists (
              select 1 from SavedPost s where s.id.postId = p.id and s.id.userId = :savedBy))
          and (:votedBy is null or exists (
              select 1 from PostVote v where v.id.postId = p.id and v.id.userId = :votedBy and v.direction = :direction))
        order by p.createdAt desc, p.id desc
        """
    )
    fun search(authorId: Long?, savedBy: Long?, votedBy: Long?, direction: Short?, pageable: Pageable): Page<Post>

    @Query(
        """
        select p from Post p
        where p.threadId in (select f.id.threadId from ThreadFollower f where f.id.userId = :userId)
        order by p.createdAt desc, p.id desc
        """
    )
    fun feed(userId: Long, pageable: Pageable): Page<Post>
}

interface PostAttachmentRepository : JpaRepository<PostAttachment, Long> {
    @Query("select a.postId, a.contentType from PostAttachment a where a.postId in :postIds")
    fun typesAmong(postIds: Collection<Long>): List<Array<Any>>
}

interface PostVoteRepository : JpaRepository<PostVote, PostVoteId> {
    @Query("select v from PostVote v where v.id.userId = :userId and v.id.postId in :postIds")
    fun byUserAmong(userId: Long, postIds: Collection<Long>): List<PostVote>
}

interface CommentRepository : JpaRepository<Comment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id = :id")
    fun findForUpdate(id: Long): Comment?

    fun findByPostIdOrderByCreatedAtAscIdAsc(postId: Long, pageable: Pageable): Page<Comment>

    @Query("select c.postId, count(c) from Comment c where c.postId in :postIds group by c.postId")
    fun countsAmong(postIds: Collection<Long>): List<Array<Any>>
}

interface CommentVoteRepository : JpaRepository<CommentVote, CommentVoteId> {
    @Query("select v from CommentVote v where v.id.userId = :userId and v.id.commentId in :commentIds")
    fun byUserAmong(userId: Long, commentIds: Collection<Long>): List<CommentVote>
}
