package hu.bme.aut.thread.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant

// Entities refer to each other by id only: no object graphs, no JPA cascades, no lazy-loading surprises.
// Deleting a thread or post relies on ON DELETE CASCADE in the schema, which never reaches users.

@Entity
@Table(name = "threads")
class TopicThread(
    @Column(nullable = false) var name: String,
    @Column(nullable = false) var description: String,
    @Column(name = "creator_id", nullable = false) val creatorId: Long,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
)

@Entity
@Table(name = "thread_images")
class ThreadImage(
    @Id @Column(name = "thread_id") val threadId: Long,
    @Column(nullable = false) var content: ByteArray,
    @Column(name = "content_type", nullable = false) var contentType: String
)

@Embeddable
data class ThreadFollowerId(
    @Column(name = "thread_id") val threadId: Long = 0,
    @Column(name = "user_id") val userId: Long = 0
) : Serializable

@Entity
@Table(name = "thread_followers")
class ThreadFollower(
    @EmbeddedId val id: ThreadFollowerId,
    @Column(name = "followed_at", nullable = false) val followedAt: Instant
)

@Entity
@Table(name = "posts")
class Post(
    @Column(name = "thread_id", nullable = false) val threadId: Long,
    @Column(name = "author_id", nullable = false) val authorId: Long,
    @Column(nullable = false) val title: String,
    @Column(nullable = false) val body: String,
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(nullable = false) val tags: List<String>,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(nullable = false) var score: Int = 0,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
)

@Entity
@Table(name = "post_attachments")
class PostAttachment(
    @Id @Column(name = "post_id") val postId: Long,
    @Column(nullable = false) var content: ByteArray,
    @Column(name = "content_type", nullable = false) var contentType: String
)

@Embeddable
data class PostVoteId(
    @Column(name = "post_id") val postId: Long = 0,
    @Column(name = "user_id") val userId: Long = 0
) : Serializable

@Entity
@Table(name = "post_votes")
class PostVote(
    @EmbeddedId val id: PostVoteId,
    @Column(nullable = false) var direction: Short
)

@Embeddable
data class SavedPostId(
    @Column(name = "user_id") val userId: Long = 0,
    @Column(name = "post_id") val postId: Long = 0
) : Serializable

@Entity
@Table(name = "saved_posts")
class SavedPost(
    @EmbeddedId val id: SavedPostId,
    @Column(name = "saved_at", nullable = false) val savedAt: Instant
)

@Entity
@Table(name = "comments")
class Comment(
    @Column(name = "post_id", nullable = false) val postId: Long,
    @Column(name = "author_id", nullable = false) val authorId: Long,
    @Column(nullable = false) val body: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(nullable = false) var score: Int = 0,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
)

@Embeddable
data class CommentVoteId(
    @Column(name = "comment_id") val commentId: Long = 0,
    @Column(name = "user_id") val userId: Long = 0
) : Serializable

@Entity
@Table(name = "comment_votes")
class CommentVote(
    @EmbeddedId val id: CommentVoteId,
    @Column(nullable = false) var direction: Short
)
