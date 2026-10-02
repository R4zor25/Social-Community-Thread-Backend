package hu.bme.aut.thread.api

import hu.bme.aut.projection.UserRef
import hu.bme.aut.thread.domain.VoteDirection
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateThreadRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 2000) val description: String = ""
)

/** Fields left out are not changed. */
data class UpdateThreadRequest(
    @field:Size(min = 1, max = 100) val name: String? = null,
    @field:Size(max = 2000) val description: String? = null
)

data class ThreadResponse(
    val id: Long,
    val name: String,
    val description: String,
    val creator: UserRef,
    val createdAt: Instant,
    val followedByMe: Boolean,
    val hasImage: Boolean
)

data class CreatePostRequest(
    @field:NotBlank @field:Size(max = 200) val title: String,
    @field:Size(max = 10_000) val body: String = "",
    @field:Size(max = 10) val tags: List<@Size(min = 1, max = 30) String> = emptyList()
)

data class PostResponse(
    val id: Long,
    val threadId: Long,
    val author: UserRef,
    val title: String,
    val body: String,
    val tags: List<String>,
    val score: Int,
    val myVote: VoteDirection?,
    val savedByMe: Boolean,
    val attachmentType: String?,
    val commentCount: Long,
    val createdAt: Instant
)

data class CreateCommentRequest(@field:NotBlank @field:Size(max = 5000) val body: String)

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val author: UserRef,
    val body: String,
    val score: Int,
    val myVote: VoteDirection?,
    val createdAt: Instant
)

data class VoteRequest(@field:NotNull val direction: VoteDirection?)
