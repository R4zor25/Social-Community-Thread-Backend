package hu.bme.aut.thread_service.models.entities

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.util.*

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ThreadPosts")
class ThreadPost(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var postId: Long? = 0,

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "topicThreadId")
    @JsonBackReference
    var topicThread: TopicThread  = TopicThread(),

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    var postTime: Date = Date(),

    @OneToMany(mappedBy = "threadPost", fetch = FetchType.EAGER, cascade = [CascadeType.REMOVE])
    var comments: MutableCollection<CommentModel> = mutableListOf(),

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinColumn(name = "user_id")
    var author : AppUser = AppUser(),

    var title: String = "",

    var description : String = "",

    var tags : MutableCollection<String> = mutableListOf(),

    var voteNumber: Int = 0,

    var postType: PostType = PostType.TEXT,

    @Lob
    var file: ByteArray = byteArrayOf()
)

enum class PostType {
    @JsonValue
    TEXT,
    @JsonValue
    IMAGE,
    @JsonValue
    GIF,
    @JsonValue
    VIDEO
}
