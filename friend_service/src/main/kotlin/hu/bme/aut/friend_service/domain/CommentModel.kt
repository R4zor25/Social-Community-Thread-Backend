package hu.bme.aut.friend_service.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.util.*

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class CommentModel(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = 0,

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "post_id")
    @JsonIgnore
    var threadPost: ThreadPost = ThreadPost(),

    @ManyToOne(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "user_id")
    var author: AppUser = AppUser(),

    var voteNumber: Int = 0,

    var commentText: String = "",

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    var commentTime: Date = Date(),

    )