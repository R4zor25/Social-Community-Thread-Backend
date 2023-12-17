package hu.bme.aut.thread_service.services

import hu.bme.aut.thread_service.models.PersonalCommentModel
import hu.bme.aut.thread_service.models.PersonalThreadPost
import hu.bme.aut.thread_service.models.PersonalTopicThread
import hu.bme.aut.thread_service.models.entities.CommentModel
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread

interface ThreadService {
    fun getRecommendedPostsForUser(userId: Long): List<PersonalThreadPost>
    fun getAllPostOfTopicThread(userId: Long, threadId: Long): List<PersonalThreadPost>
    fun getThreadPost(userId: Long, threadId: Long, postId: Long): PersonalThreadPost
    fun getThreadDetails(userId: Long, threadId: Long): PersonalTopicThread
    fun getUsersFollowedThreads(userId: Long): List<TopicThread>
    fun getUsersSavedPosts(userId: Long): List<PersonalThreadPost>
    fun getUsersUpvotedPosts(userId: Long): List<PersonalThreadPost>
    fun getUsersDownvotedPosts(userId: Long): List<PersonalThreadPost>
    fun getPostsByUser(userId: Long): List<PersonalThreadPost>
    fun savePost(userId: Long, threadId: Long, postId: Long)
    fun unsavePost(userId: Long, threadId: Long, postId: Long)
    fun followThread(userId: Long, threadId: Long)
    fun unfollowThread(userId: Long, threadId: Long)
    fun deletePost(userId: Long, threadId: Long, postId: Long)
    fun deleteThread(userId: Long, threadId: Long)
    fun modifyThreadData(userId: Long, threadId: Long, topicThread: TopicThread)
    fun getTopicThreadsFiltered(containsString: String): List<TopicThread>
    fun getPostInTopicThreadFiltered(userId: Long, threadId: Long, containsString: String): List<PersonalThreadPost>
    fun upvotePost(userId: Long, threadId: Long, postId: Long)
    fun downvotePost(userId: Long, threadId: Long, postId: Long)
    fun postComment(userId: Long, threadId: Long, postId: Long, commentModel: CommentModel) : PersonalCommentModel
    fun createPost(userId: Long, threadId: Long, threadPost: ThreadPost)
    fun createThread(userId : Long, topicThread: TopicThread)
    fun upvoteComment(userId: Long, threadId: Long, postId: Long, commentId: Long)
    fun downvoteComment(userId: Long, threadId: Long, postId: Long, commentId: Long)
}