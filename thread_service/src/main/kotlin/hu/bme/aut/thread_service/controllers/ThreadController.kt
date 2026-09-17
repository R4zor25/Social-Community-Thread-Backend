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
        return ResponseEntity.ok().body(threadService.getRecommendedPostsForUser(userId))
    }

    @GetMapping("/{userId}/{threadId}/posts")
    fun getTopicThreadPosts(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getAllPostOfTopicThread(userId, threadId))
    }

    @GetMapping("/{userId}/{threadId}/details")
    fun getTopicThreadDetails(@PathVariable userId: Long, @PathVariable threadId: Long) : ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getThreadDetails(userId, threadId))
    }

    @GetMapping("/{userId}/{threadId}/{postId}/details")
    fun getPostDetails(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getThreadPost(userId, threadId, postId))
    }

    @GetMapping("/{userId}/followed")
    fun getFollowedThreads(@PathVariable userId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getUsersFollowedThreads(userId))
    }

    @GetMapping("/{userId}/saved")
    fun getSavedPosts(@PathVariable userId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getUsersSavedPosts(userId))
    }

    @GetMapping("/{userId}/upvoted")
    fun getUpvotedPosts(@PathVariable userId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getUsersUpvotedPosts(userId))
    }

    @GetMapping("/{userId}/downvoted")
    fun getDownvotedPosts(@PathVariable userId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getUsersDownvotedPosts(userId))
    }

    @GetMapping("/{userId}/posts")
    fun getPostsByUser(@PathVariable userId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getPostsByUser(userId))
    }

    @PutMapping("/{userId}/{threadId}/{postId}/save")
    fun savePost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long) : ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.savePost(userId, threadId, postId))
    }

    @PutMapping("/{userId}/{threadId}/{postId}/unsave")
    fun unsavePost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long) : ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.unsavePost(userId, threadId, postId))
    }


    @PutMapping("/{userId}/{threadId}/follow")
    fun followThread(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        threadService.followThread(userId, threadId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{userId}/{threadId}/unfollow")
    fun unfollowThread(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        threadService.unfollowThread(userId, threadId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{userId}/{threadId}/{postId}/delete")
    fun deletePost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        threadService.deletePost(userId, threadId, postId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{userId}/{threadId}/delete")
    fun deleteThread(@PathVariable userId: Long, @PathVariable threadId: Long): ResponseEntity<Any> {
        threadService.deleteThread(userId, threadId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{userId}/{threadId}/{postId}/comment")
    fun postComment(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long, @RequestBody commentModel: CommentModel) : ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.postComment(userId, threadId, postId, commentModel))
    }

    @PutMapping("/{userId}/{threadId}/{postId}/{commentId}/upvote")
    fun upvoteComment(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long, @PathVariable commentId: Long) : ResponseEntity<Any> {
        threadService.upvoteComment(userId, threadId, postId, commentId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{userId}/{threadId}/{postId}/{commentId}/downvote")
    fun downvoteComment(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long, @PathVariable commentId: Long) : ResponseEntity<Any> {
        threadService.downvoteComment(userId, threadId, postId, commentId)
        return ResponseEntity.ok().build()
    }



    @PutMapping("/{userId}/{threadId}/modify")
    fun modifyThreadData(@PathVariable userId: Long, @PathVariable threadId: Long, @RequestBody topicThread : TopicThread): ResponseEntity<Any> {
        threadService.modifyThreadData(userId, threadId, topicThread)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/search")
    fun getFilteredThreads(@RequestParam containsString: String): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getTopicThreadsFiltered(containsString))
    }

    @GetMapping("/{userId}/{threadId}/post/search")
    fun getFilteredPosts(@PathVariable userId: Long, @PathVariable threadId: Long, @RequestParam containsString: String): ResponseEntity<Any> {
        return ResponseEntity.ok().body(threadService.getPostInTopicThreadFiltered(userId, threadId, containsString))
    }


    @PutMapping("/{userId}/{threadId}/{postId}/upvote")
    fun upvoteThreadPost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        threadService.upvotePost(userId, threadId, postId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{userId}/{threadId}/{postId}/downvote")
    fun downvoteThreadPost(@PathVariable userId: Long, @PathVariable threadId: Long, @PathVariable postId: Long): ResponseEntity<Any> {
        threadService.downvotePost(userId, threadId, postId)
        return ResponseEntity.ok().build()
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
        threadService.createPost(userId, threadId, threadPost)
        return ResponseEntity.ok().build()

    }

    @PostMapping("/{userId}/create")
    fun createThread(@PathVariable userId: Long, @RequestBody topicThread: TopicThread) : ResponseEntity<Any> {
        threadService.createThread(userId, topicThread)
        return ResponseEntity.ok().build()
    }


}