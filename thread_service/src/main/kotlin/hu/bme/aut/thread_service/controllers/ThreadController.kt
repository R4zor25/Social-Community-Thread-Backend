package hu.bme.aut.thread_service.controllers

import hu.bme.aut.thread_service.models.entities.CommentModel
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread
import hu.bme.aut.thread_service.services.ThreadService
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/thread")
@Slf4j
@RequiredArgsConstructor
//@PreAuthorize("isAuthenticated()")
class ThreadController(
    private var threadService: ThreadService
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    var UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/uploads"

    @GetMapping("/{userId}/posts/recommended")
    fun getRecommendedPosts(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getRecommendedPostsForUser(userId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/{threadId}/posts")
    fun getTopicThreadPosts(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getAllPostOfTopicThread(userId, threadId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/{threadId}/details")
    fun getTopicThreadDetails(@PathVariable userId: Long, @PathVariable threadId: Long) : ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getThreadDetails(userId, threadId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/{threadId}/{postId}/details")
    fun getPostDetails(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getThreadPost(userId, threadId, postId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/followed")
    fun getFollowedThreads(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getUsersFollowedThreads(userId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/saved")
    fun getSavedPosts(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getUsersSavedPosts(userId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/upvoted")
    fun getUpvotedPosts(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getUsersUpvotedPosts(userId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/downvoted")
    fun getDownvotedPosts(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getUsersDownvotedPosts(userId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/posts")
    fun getPostsByUser(@PathVariable userId: Long): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getPostsByUser(userId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PutMapping("/{userId}/{threadId}/{postId}/save")
    fun savePost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long) : ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.savePost(userId, threadId, postId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PutMapping("/{userId}/{threadId}/{postId}/unsave")
    fun unsavePost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long) : ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.unsavePost(userId, threadId, postId))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }


    @PutMapping("/{userId}/{threadId}/follow")
    fun followThread(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        return try {
            threadService.followThread(userId, threadId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PutMapping("/{userId}/{threadId}/unfollow")
    fun unfollowThread(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        return try {
            threadService.unfollowThread(userId, threadId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @DeleteMapping("/{userId}/{threadId}/{postId}/delete")
    fun deletePost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        return try {
            threadService.deletePost(userId, threadId, postId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @DeleteMapping("/{userId}/{threadId}/delete")
    fun deleteThread(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        return try {
            threadService.deleteThread(userId, threadId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/{threadId}/{postId}/comment")
    fun postComment(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long, @RequestBody commentModel: CommentModel) : ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.postComment(userId, threadId, postId, commentModel))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PutMapping("/{userId}/{threadId}/{postId}/{commentId}/upvote")
    fun upvoteComment(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long, @PathVariable commentId: Long) : ResponseEntity<Any> {
        return try {
            threadService.upvoteComment(userId, threadId, postId, commentId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PutMapping("/{userId}/{threadId}/{postId}/{commentId}/downvote")
    fun downvoteComment(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long, @PathVariable commentId: Long) : ResponseEntity<Any> {
        return try {
            threadService.downvoteComment(userId, threadId, postId, commentId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }



    @PutMapping("/{userId}/{threadId}/modify")
    fun modifyThreadData(@PathVariable userId: Long, @PathVariable threadId: Long, @RequestBody topicThread : TopicThread): ResponseEntity<Any> {
        return try {
            threadService.modifyThreadData(userId, threadId, topicThread)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/search")
    fun getFilteredThreads(@RequestParam containsString: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getTopicThreadsFiltered(containsString))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @GetMapping("/{userId}/{threadId}/post/search")
    fun getFilteredPosts(@PathVariable userId: Long, @PathVariable threadId: Long, @RequestParam containsString: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(threadService.getPostInTopicThreadFiltered(userId, threadId, containsString))
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }


    @PutMapping("/{userId}/{threadId}/{postId}/upvote")
    fun upvoteThreadPost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        return try {
            threadService.upvotePost(userId, threadId, postId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PutMapping("/{userId}/{threadId}/{postId}/downvote")
    fun downvoteThreadPost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        return try {
            threadService.downvotePost(userId, threadId, postId)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }

    @PostMapping("/{userId}/{threadId}/create")
    //fun createPost(@PathVariable userId: Long, @PathVariable threadId: Long, @RequestPart threadPostDTO: String, @RequestPart(required = false, value = "file") file: MultipartFile?) {
    fun createPost(@PathVariable userId: Long, @PathVariable threadId: Long, @RequestBody threadPost: ThreadPost) : ResponseEntity<Any> {
        /*val objectMapper = ObjectMapper()
        val post = objectMapper.readValue(threadPostDTO, ThreadPostDTO::class.java)
        if(post.postType != PostType.TEXT) {
            val fileNames = StringBuilder()
            val fileNameAndPath: Path = Paths.get(UPLOAD_DIRECTORY, file?.originalFilename)
            fileNames.append(file?.originalFilename)
            Files.write(fileNameAndPath, file?.bytes)
        }
        */
        return try {
            threadService.createPost(userId, threadId, threadPost)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.status(404).body(e.localizedMessage)
        }

    }

    @PostMapping("/{userId}/create")
    fun createThread(@PathVariable userId: Long, @RequestBody topicThread: TopicThread) : ResponseEntity<Any> {
        return try {
            threadService.createThread(userId, topicThread)
            ResponseEntity.ok().build()
        } catch (e: Exception){
            ResponseEntity.status(404).body(e.localizedMessage)
        }
    }


}