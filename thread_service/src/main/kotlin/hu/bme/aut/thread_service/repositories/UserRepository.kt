package hu.bme.aut.thread_service.repositories

import hu.bme.aut.thread_service.models.entities.AppUser
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByUserName(userName: String): List<AppUser>
    fun findByEmail(email: String): List<AppUser>

    /** Users that saved or voted on the post or voted on one of its comments. */
    @Query("""
        select distinct u from AppUser u
        where :post member of u.savedPosts
           or :post member of u.upvotedPosts
           or :post member of u.downvotedPosts
           or exists (
               select c from CommentModel c
               where c.threadPost = :post and (c member of u.upvotedComments or c member of u.downvotedComments)
           )
    """)
    fun findUsersReferencing(@Param("post") post: ThreadPost): List<AppUser>

    @Query("select u from AppUser u where :thread member of u.followedThreads")
    fun findFollowers(@Param("thread") thread: TopicThread): List<AppUser>
}
