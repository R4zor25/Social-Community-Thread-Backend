package hu.bme.aut.thread_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.thread_service.models.entities.AppUser
import hu.bme.aut.thread_service.models.entities.CommentModel
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread
import hu.bme.aut.thread_service.repositories.CommentRepository
import hu.bme.aut.thread_service.repositories.PostRepository
import hu.bme.aut.thread_service.repositories.ThreadRepository
import hu.bme.aut.thread_service.repositories.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.*
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ThreadControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
) {

    var path = "src/test/resources"

    @TestConfiguration
    internal class DatabasePreparation @Autowired constructor(
        val userRepository: UserRepository,
        val threadRepository: ThreadRepository,
        val postRepository: PostRepository,
        val commentRepository: CommentRepository
    ) {
        @EventListener(ApplicationStartedEvent::class)
        fun preparedDbForTheTest() {
            //val file = java.io.File("src/test/resources/test.png")
            val user1 = AppUser(userName = "test1", email = "test1", password = "test")
            val topicThread = TopicThread().apply {
                name = "TestThread"
                description = "TestThreadDescription"
                //threadImage = file.readBytes()
            }
            val topicThread1 = TopicThread().apply {
                name = "TestThread1"
                description = "TestThreadDescription1"
               // threadImage = file.readBytes()
            }
            val threadPost1 = ThreadPost().apply {
                postTime = Date()
                this.topicThread = topicThread
                this.author = user1
                this.title = "TestTitle"
                this.description = "TestDescription"
                this.tags = mutableListOf("tag1", "tag2")
                this.voteNumber = 1
                //this.file = file.readBytes()
            }
            val threadPost2 = ThreadPost().apply {
                postTime = Date()
                this.topicThread = topicThread
                this.author = user1
                this.title = "TestTitle2"
                this.description = "TestDescription2"
                this.tags = mutableListOf("tag1", "tag2")
                this.voteNumber = -1
                //this.file = file.readBytes()
            }
            val commentModel = CommentModel().apply {
                this.threadPost = threadPost1
                this.author = user1
                this.voteNumber = 0
                this.commentText = "TestCommentText"
            }

            user1.savedPosts.add(threadPost1)
            user1.downvotedPosts.add(threadPost2)
            user1.upvotedPosts.add(threadPost1)
            user1.followedThreads.add(topicThread)

            threadPost1.apply {
                this.comments.add(commentModel)
            }
            topicThread.apply {
                this.threadposts.add(threadPost1)
                this.threadposts.add(threadPost2)
            }

            val a = userRepository.save(user1)
            val b =commentRepository.save(commentModel)
            val b1 =postRepository.save(threadPost1)
            val b2 =postRepository.save(threadPost2)
            val b3 =threadRepository.save(topicThread)
            val b4 =threadRepository.save(topicThread1)
        }
    }

    @Test
    fun testGetRecommendedPostsSuccessful() {
        val url = "/api/thread/1/posts/recommended"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    jsonPath("$.size()") { value(2) }
                }
            }
    }

    @Test
    fun testGetRecommendedPostsUnsuccessful() {
        val url = "/api/thread/9/posts/recommended"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }
    }

    @Test
    fun testGetThreadPosts() {
        val url = "/api/thread/1/posts"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(2) }
                jsonPath("$[0].title") { value("TestTitle2") }
                jsonPath("$[0].description") { value("TestDescription2") }
            }
        }
    }

    @Test
    fun getAllTopicThreadPostsSuccessful(){
        val url = "/api/thread/1/1/posts"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(2) }
            }
        }
    }

    @Test
    fun getAllTopicThreadPostsUnsuccessful(){
        val url = "/api/thread/1/9/posts"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }
    }

    @Test
    fun testGetThreadPostsUnsuccessful() {
        val url = "/api/thread/9/posts"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }
    }

    @Test
    fun testGetThreadSuccessful() {
        val url = "/api/thread/1/1/details"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.name") { value("TestThread") }
            }
        }
    }

    @Test
    fun testGetThreadUnsuccessful() {
        val url = "/api/thread/1/9/details"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testGetPostDetailsSuccessful() {
        val url = "/api/thread/1/1/1/details"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.title") { value("TestTitle2") }
                jsonPath("$.description") { value("TestDescription2") }
                jsonPath("$.voteNumber") { value(-1) }
            }
        }
    }

    @Test
    fun testGetPostDetailsUnsuccessful() {
        val url = "/api/thread/1/1/9/details"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url2 = "/api/thread/1/9/1/details"
        val get2 = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get2.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testGetFollowedThreadsSuccessful() {
        val url = "/api/thread/1/followed"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(1) }
                jsonPath("$[0].name") { value("TestThread") }
            }
        }
    }

    @Test
    fun testGetFollowedThreadsUnsuccessful() {
        val url = "/api/thread/9/followed"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testGetSavedPostsSuccessful() {
        val url = "/api/thread/1/saved"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(1) }
                jsonPath("$[0].title") { value("TestTitle") }
                jsonPath("$[0].description") { value("TestDescription") }
            }
        }
    }

    @Test
    fun testGetSavedPostsUnsuccessful() {
        val url = "/api/thread/9/saved"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testGetUpvotedPostsSuccessful(){
        val url = "/api/thread/1/upvoted"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(1) }
                jsonPath("$[0].title") { value("TestTitle") }
            }
        }
    }

    @Test
    fun testGetUpvotedPostsUnsuccessful(){
        val url = "/api/thread/9/upvoted"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testGetDownvotedPostsSuccessful(){
        val url = "/api/thread/1/downvoted"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(1) }
                jsonPath("$[0].title") { value("TestTitle2") }
            }
        }
    }

    @Test
    fun testGetDownvotedPostsUnsuccessful(){
        val url = "/api/thread/9/downvoted"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testGetPostsByUserSuccessful(){
        val url = "/api/thread/1/posts"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(2) }
            }
        }
    }

    @Test
    fun testGetPostsByUserUnsuccessful(){
        val url = "/api/thread/9/posts"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testSavePostSuccessful(){
        val url = "/api/thread/1/1/2/save"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url2 = "/api/thread/1/saved"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(2) }
            }
        }
    }

    @Test
    fun testSavePostUnsuccessful(){
        val url = "/api/thread/9/1/2/save"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/9/2/save"
        val put1 = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put1.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url2 = "/api/thread/1/1/9/save"
        val put2 = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put2.andDo { print() }.andExpect {
            status { isNotFound() }
        }

    }

    @Test
    fun testUnsavePostSuccessful() {
        val url = "/api/thread/1/1/2/unsave"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url2 = "/api/thread/1/saved"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(0) }
            }
        }
    }


    @Test
    fun testUnsavePostUnsuccessful() {
        val url = "/api/thread/9/1/2/unsave"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/9/2/unsave"
        val put1 = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put1.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url2 = "/api/thread/1/1/9/unsave"
        val put2 = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put2.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testFollowThreadSuccessful() {
        val url = "/api/thread/1/2/follow"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
            }
        }

        val url2 = "/api/thread/1/followed"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(2) }
            }
        }
    }

    @Test
    fun testFollowThreadUnsuccessful() {
        val url = "/api/thread/1/9/follow"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testUnfollowThreadSuccessful() {
        val url = "/api/thread/1/1/unfollow"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
            }
        }

        val url2 = "/api/thread/1/followed"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(0) }
            }
        }
    }

    @Test
    fun testUnfollowThreadUnsuccessful() {
        val url = "/api/thread/1/9/unfollow"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testDeletePostSuccessful() {
        val url = "/api/thread/1/1/1/delete"
        val delete = mockMvc.delete(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        delete.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url2 = "/api/thread/1/posts"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }
    }

    @Test
    fun testDeletePostUnsuccessful() {
        val url = "/api/thread/1/1/9/delete"
        val delete = mockMvc.delete(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        delete.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/9/1/delete"
        val delete1 = mockMvc.delete(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        delete1.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testDeleteThreadSuccessful() {
        val url = "/api/thread/1/1/delete"
        val delete = mockMvc.delete(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        delete.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url2 = "/api/thread/1/1/details"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }
    }

    @Test
    fun testDeleteThreadUnsuccessful() {
        val url = "/api/thread/1/9/delete"
        val delete = mockMvc.delete(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        delete.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testPostCommentSuccessful() {
        val url = "/api/thread/1/1/1/comment"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CommentModel(
                    commentText = "TestComment"
                )
            )
        }

        post.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url2 = "/api/thread/1/1/2/details"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.comments.size()") { value(1) }
            }
        }
    }

    @Test
    fun testPostCommentUnsuccessful() {
        val url = "/api/thread/1/1/9/comment"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                CommentModel(
                    commentText = "TestComment"
                )
            )
        }

        post.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testUpvoteCommentSuccessful() {
        val url = "/api/thread/1/1/1/1/upvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url2 = "/api/thread/1/1/2/details"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.comments[0].voteNumber") { value(1) }
            }
        }
    }

    @Test
    fun testUpvoteCommentUnsuccessful() {
        val url = "/api/thread/1/1/1/9/upvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/1/9/1/upvote"
        val put1 = mockMvc.put(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        put1.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url2 = "/api/thread/1/9/1/1/upvote"
        val put2 = mockMvc.put(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        put2.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url3 = "/api/thread/9/1/1/1/upvote"
        val put3 = mockMvc.put(url3) {
            contentType = MediaType.APPLICATION_JSON
        }

        put3.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun testDownvoteCommentSuccessful() {
        val url = "/api/thread/1/1/1/1/downvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url2 = "/api/thread/1/1/2/details"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.comments[0].voteNumber") { value(-1) }
            }
        }
    }

    @Test
    fun testDownvoteCommentUnsuccessful() {
        val url = "/api/thread/1/1/1/9/downvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/1/9/1/downvote"
        val put1 = mockMvc.put(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        put1.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url2 = "/api/thread/1/9/1/1/downvote"
        val put2 = mockMvc.put(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        put2.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url3 = "/api/thread/9/1/1/1/downvote"
        val put3 = mockMvc.put(url3) {
            contentType = MediaType.APPLICATION_JSON
        }

        put3.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }



    @Test
    fun testModifyThreadSuccessful(){
        val url = "/api/thread/1/1/modify"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                TopicThread().apply {
                    topicThreadId = 1
                    name = "TestModifiedTitle"
                    description = "TestModifiedDescription"
                }
            )
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
            }
        }

        val url2 = "/api/thread/1/1/details"
        val get = mockMvc.get(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.name") { value("TestModifiedTitle") }
                jsonPath("$.description") { value("TestModifiedDescription") }
            }
        }
    }

    @Test
    fun testModifyThreadUnsuccessful(){
        val url = "/api/thread/1/8/modify"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                TopicThread().apply {
                    topicThreadId = 1
                    name = "TestModifiedTitle"
                    description = "TestModifiedDescription"
                }
            )
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun getFilteredThreadsSuccessful(){
        val url = "/api/thread/search?containsString=TestThread1"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(1) }
                jsonPath("$[0].name") { value("TestThread1") }
            }
        }
    }

    @Test
    fun getFilteredThreadsUnsuccessful(){
        val url = "/api/thread/search?containsString=TestThreadasdasdasda"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(0) }
            }
        }
    }

    @Test
    fun getFilteredPostsSuccessful(){
        val url = "/api/thread/1/1/post/search?containsString=TestTitle2"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.size()") { value(1) }
                jsonPath("$[0].title") { value("TestTitle2") }
            }
        }
    }

    @Test
    fun getFilteredPostsUnsuccessful(){
        val url = "/api/thread/9/1/post/search?containsString=TestPost"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun upvoteThreadPostSuccessful() {
        val url = "/api/thread/1/1/1/upvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url1 = "/api/thread/1/1/1/details"
        val get = mockMvc.get(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.voteNumber") { value(1) }
            }
        }
    }

    @Test
    fun upvoteThreadPostUnsuccessful() {
        val url = "/api/thread/9/1/1/upvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/9/1/upvote"
        val put1 = mockMvc.put(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        put1.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url2 = "/api/thread/1/1/9/upvote"
        val put2 = mockMvc.put(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        put2.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun downvoteThreadPostSuccessful() {
        val url = "/api/thread/1/1/1/downvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url1 = "/api/thread/1/1/1/details"
        val get = mockMvc.get(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.voteNumber") { value(0) }
            }
        }
    }

    @Test
    fun downvoteThreadPostUnsuccessful() {
        val url = "/api/thread/9/1/1/downvote"
        val put = mockMvc.put(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        put.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/9/1/downvote"
        val put1 = mockMvc.put(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        put1.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url2 = "/api/thread/1/1/9/downvote"
        val put2 = mockMvc.put(url2) {
            contentType = MediaType.APPLICATION_JSON
        }

        put2.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun createThreadSuccessful() {
        val url = "/api/thread/1/create"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                TopicThread(
                    name = "TestThread",
                    description = "TestDescription",
                )
            )
        }

        post.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url1 = "/api/thread/1/3/details"
        val get = mockMvc.get(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.name") { value("TestThread") }
                jsonPath("$.description") { value("TestDescription") }
            }
        }
    }

        @Test
        fun createThreadUnsuccessful() {
            val url = "/api/thread/9/create"
            val post = mockMvc.post(url) {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    TopicThread(
                        name = "TestThread",
                        description = "TestDescription",
                    )
                )
            }

            post.andDo { print() }.andExpect {
                status { isNotFound() }
            }
        }

    @Test
    fun testCreatePost(){
        val url = "/api/thread/1/1/create"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ThreadPost(
                    title = "NewTestTitle"
                )
            )
        }

        post.andDo { print() }.andExpect {
            status { isOk() }
        }

        val url1 = "/api/thread/1/1/3/details"
        val get = mockMvc.get(url1) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.title") { value("NewTestTitle") }
            }
        }
    }

    @Test
    fun testCreatePostUnsuccessful() {
        val url = "/api/thread/9/1/create"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ThreadPost(
                    title = "NewTestTitle"
                )
            )
        }

        post.andDo { print() }.andExpect {
            status { isNotFound() }
        }

        val url1 = "/api/thread/1/9/create"
        val post1 = mockMvc.post(url1) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ThreadPost(
                    title = "NewTestTitle"
                )
            )
        }

        post1.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }


}


