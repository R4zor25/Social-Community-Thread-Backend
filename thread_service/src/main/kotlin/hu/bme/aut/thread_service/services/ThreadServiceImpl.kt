package hu.bme.aut.thread_service.services

import hu.bme.aut.thread_service.models.PersonalCommentModel
import hu.bme.aut.thread_service.models.PersonalThreadPost
import hu.bme.aut.thread_service.models.PersonalTopicThread
import hu.bme.aut.thread_service.models.entities.CommentModel
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread
import hu.bme.aut.thread_service.models.entities.VoteType
import hu.bme.aut.thread_service.repositories.CommentRepository
import hu.bme.aut.thread_service.repositories.PostRepository
import hu.bme.aut.thread_service.repositories.ThreadRepository
import hu.bme.aut.thread_service.repositories.UserRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@OptIn(ExperimentalStdlibApi::class)
@Service
@RequiredArgsConstructor
@Transactional
open class ThreadServiceImpl(
    private val threadRepository: ThreadRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository
) : ThreadService {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun getRecommendedPostsForUser(userId: Long): List<PersonalThreadPost> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val threads = user.followedThreads
        val recommendedPosts: MutableList<ThreadPost> = mutableListOf()
        for (thread in threads) {
            recommendedPosts.addAll(thread.threadposts)
        }
        recommendedPosts.sortByDescending { it.postTime }
        return recommendedPosts.map { post ->
            val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
            personalPost.apply {
                this.comments = post.comments.map {
                    val voteType = when {
                        user.upvotedComments.contains(it) -> VoteType.UPVOTED
                        user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                        else -> VoteType.CLEAR
                    }
                    PersonalCommentModel().apply {
                        initPersonalCommentModel(it, voteType)
                    }
                }.toMutableList()
            }
        }
    }

    override fun getAllPostOfTopicThread(userId: Long, threadId: Long): List<PersonalThreadPost> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        return thread.threadposts.map { post ->
            val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
            personalPost.apply {
                this.comments = post.comments.map {
                    val voteType = when {
                        user.upvotedComments.contains(it) -> VoteType.UPVOTED
                        user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                        else -> VoteType.CLEAR
                    }
                    PersonalCommentModel().apply {
                        initPersonalCommentModel(it, voteType)
                    }
                }.toMutableList()
            }
        }.toList()
    }

    override fun getThreadPost(userId: Long, threadId: Long, postId: Long): PersonalThreadPost {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        val post = postRepository.findById(postId).getOrNull() ?: throw EntityNotFoundException("Post does not exist")
        val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
        personalPost.apply {
            this.comments = post.comments.map {
                val voteType = when {
                    user.upvotedComments.contains(it) -> VoteType.UPVOTED
                    user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                    else -> VoteType.CLEAR
                }
                PersonalCommentModel().apply {
                    initPersonalCommentModel(it, voteType)
                }
            }.toMutableList()
        }
        return personalPost
    }


    override fun getThreadDetails(userId: Long, threadId: Long): PersonalTopicThread {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        return PersonalTopicThread().initPersonalTopicThread(thread, user)
    }

    override fun getUsersFollowedThreads(userId: Long): List<TopicThread> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        return user.followedThreads.toList()
    }

    override fun getUsersSavedPosts(userId: Long): List<PersonalThreadPost> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        return user.savedPosts.map { post ->
            val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
            personalPost.apply {
                this.comments = post.comments.map {
                    val voteType = when {
                        user.upvotedComments.contains(it) -> VoteType.UPVOTED
                        user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                        else -> VoteType.CLEAR
                    }
                    PersonalCommentModel().apply {
                        initPersonalCommentModel(it, voteType)
                    }
                }.toMutableList()
            }
        }
    }

    override fun getUsersUpvotedPosts(userId: Long): List<PersonalThreadPost> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        return user.upvotedPosts.map { post ->
            val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
            personalPost.apply {
                this.comments = post.comments.map {
                    val voteType = when {
                        user.upvotedComments.contains(it) -> VoteType.UPVOTED
                        user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                        else -> VoteType.CLEAR
                    }
                    PersonalCommentModel().apply {
                        initPersonalCommentModel(it, voteType)
                    }
                }.toMutableList()
            }
        }
    }

    override fun getUsersDownvotedPosts(userId: Long): List<PersonalThreadPost> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        return user.downvotedPosts.map { post ->
            val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
            personalPost.apply {
                this.comments = post.comments.map {
                    val voteType = when {
                        user.upvotedComments.contains(it) -> VoteType.UPVOTED
                        user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                        else -> VoteType.CLEAR
                    }
                    PersonalCommentModel().apply {
                        initPersonalCommentModel(it, voteType)
                    }
                }.toMutableList()
            }
        }
    }

    override fun getPostsByUser(userId: Long): List<PersonalThreadPost> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        return postRepository.findAll().filter { it.author.userId == userId }.map { post ->
            val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
            personalPost.apply {
                this.comments = post.comments.map {
                    val voteType = when {
                        user.upvotedComments.contains(it) -> VoteType.UPVOTED
                        user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                        else -> VoteType.CLEAR
                    }
                    PersonalCommentModel().apply {
                        initPersonalCommentModel(it, voteType)
                    }
                }.toMutableList()
            }
        }
    }

    override fun savePost(userId: Long, threadId: Long, postId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        user.savedPosts.add(postRepository.findById(postId).get())
        userRepository.save(user)
    }

    override fun unsavePost(userId: Long, threadId: Long, postId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        user.savedPosts.remove(postRepository.findById(postId).get())
        userRepository.save(user)
    }

    override fun followThread(userId: Long, threadId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        user.followedThreads.add(threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist"))
        userRepository.save(user)
    }

    override fun unfollowThread(userId: Long, threadId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        user.followedThreads.remove(threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist"))
        userRepository.save(user)
    }

    override fun deletePost(userId: Long, threadId: Long, postId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val post = postRepository.findById(postId).getOrNull() ?: throw EntityNotFoundException("Post does not exist")
        postRepository.deleteById(postId)
    }

    override fun deleteThread(userId: Long, threadId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        threadRepository.deleteById(threadId)
    }

    override fun modifyThreadData(userId: Long, threadId: Long, topicThread: TopicThread) {
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        threadRepository.save(topicThread)
    }

    override fun getTopicThreadsFiltered(containsString: String): List<TopicThread> {
        val threads = threadRepository.findAll()
        return threads.filter { it.name.contains(containsString) }
    }

    override fun getPostInTopicThreadFiltered(userId: Long, threadId: Long, containsString: String): List<PersonalThreadPost> {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val posts = postRepository.findAll()
        return posts.filter { it.topicThread.topicThreadId == threadId && it.title.contains(containsString) }
            .map { post ->
                val personalPost = PersonalThreadPost().initPersonalThreadPost(post, user)
                personalPost.apply {
                    this.comments = post.comments.map {
                        val voteType = when {
                            user.upvotedComments.contains(it) -> VoteType.UPVOTED
                            user.downvotedComments.contains(it) -> VoteType.DOWNVOTED
                            else -> VoteType.CLEAR
                        }
                        PersonalCommentModel().apply {
                            initPersonalCommentModel(it, voteType)
                        }
                    }.toMutableList()
                }
            }
    }

    override fun upvotePost(userId: Long, threadId: Long, postId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        val post = postRepository.findById(postId).getOrNull() ?: throw EntityNotFoundException("Post does not exist")
        if (user.upvotedPosts.contains(post)) {
            post.voteNumber -= 1
            postRepository.save(post)
            user.upvotedPosts.remove(post)
        } else if (user.downvotedPosts.contains(post)) {
            post.voteNumber += 2
            postRepository.save(post)
            user.downvotedPosts.remove(post)
            user.upvotedPosts.add(post)
        } else {
            post.voteNumber += 1
            postRepository.save(post)
            user.upvotedPosts.add(post)
        }
        userRepository.save(user)
    }

    override fun downvotePost(userId: Long, threadId: Long, postId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        val post = postRepository.findById(postId).getOrNull() ?: throw EntityNotFoundException("Post does not exist")
        if (user.upvotedPosts.contains(post)) {
            post.voteNumber -= 2
            postRepository.save(post)
            user.upvotedPosts.remove(post)
            user.downvotedPosts.add(post)
        } else if (user.downvotedPosts.contains(post)) {
            post.voteNumber += 1
            postRepository.save(post)
            user.downvotedPosts.remove(post)
        } else {
            post.voteNumber -= 1
            postRepository.save(post)
            user.downvotedPosts.add(post)
        }
        userRepository.save(user)
    }

    override fun postComment(userId: Long, threadId: Long, postId: Long, commentModel: CommentModel): PersonalCommentModel {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        val post = postRepository.findById(postId).getOrNull() ?: throw EntityNotFoundException("Post does not exist")
        commentModel.apply {
            this.threadPost = post
            this.author = user
        }
        val commentResult = commentRepository.save(commentModel)
        post.comments.add(commentResult)
        postRepository.save(post)
        val voteType = when {
            user.upvotedComments.contains(commentResult) -> VoteType.UPVOTED
            user.downvotedComments.contains(commentResult) -> VoteType.DOWNVOTED
            else -> VoteType.CLEAR
        }
        return PersonalCommentModel().apply {
            initPersonalCommentModel(commentResult, voteType)
        }
    }

    override fun createPost(userId: Long, threadId: Long, threadPost: ThreadPost) {
        /*val threadPost = ThreadPost(
            topicThread = threadRepository.findById(threadPostDTO.topicThreadId).get(),
            postTime = threadPostDTO.postTime,
            title = threadPostDTO.title,
            description = threadPostDTO.postText,
            file = file
        )*/
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        threadPost.apply {
            author = user
            topicThread = thread
        }
        postRepository.save(threadPost)
    }

    override fun createThread(userId: Long, topicThread: TopicThread) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        threadRepository.save(topicThread)
    }

    override fun upvoteComment(userId: Long, threadId: Long, postId: Long, commentId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val post = postRepository.findById(postId).getOrNull() ?: throw EntityNotFoundException("Post does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        val comment = commentRepository.findById(commentId).getOrNull() ?: throw EntityNotFoundException("Comment does not exist")
        if (user.upvotedComments.contains(comment)) {
            comment.voteNumber -= 1
            commentRepository.save(comment)
            user.upvotedComments.remove(comment)
        } else if (user.downvotedComments.contains(comment)) {
            comment.voteNumber += 2
            commentRepository.save(comment)
            user.downvotedComments.remove(comment)
            user.upvotedComments.add(comment)
        } else {
            comment.voteNumber += 1
            commentRepository.save(comment)
            user.upvotedComments.add(comment)
        }
        userRepository.save(user)
    }

    override fun downvoteComment(userId: Long, threadId: Long, postId: Long, commentId: Long) {
        val user = userRepository.findById(userId).getOrNull() ?: throw EntityNotFoundException("User does not exist")
        val post = postRepository.findById(postId).getOrNull() ?: throw EntityNotFoundException("Post does not exist")
        val thread = threadRepository.findById(threadId).getOrNull() ?: throw EntityNotFoundException("Thread does not exist")
        val comment = commentRepository.findById(commentId).getOrNull() ?: throw EntityNotFoundException("Comment does not exist")
        if (user.upvotedComments.contains(comment)) {
            comment.voteNumber -= 2
            commentRepository.save(comment)
            user.upvotedComments.remove(comment)
            user.downvotedComments.add(comment)
        } else if (user.downvotedComments.contains(comment)) {
            comment.voteNumber += 1
            commentRepository.save(comment)
            user.downvotedComments.remove(comment)
        } else {
            comment.voteNumber -= 1
            commentRepository.save(comment)
            user.downvotedComments.add(comment)
        }
        userRepository.save(user)
    }
}