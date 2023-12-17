package hu.bme.aut.thread_service.models

import com.fasterxml.jackson.annotation.JsonFormat
import hu.bme.aut.thread_service.models.entities.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.util.*

@Data
@NoArgsConstructor
@AllArgsConstructor
data class PersonalThreadPost(
    var postId: Long? = 0,
    var topicThread: TopicThread = TopicThread(),
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    var postTime: Date = Date(),
    var comments: MutableCollection<PersonalCommentModel> = mutableListOf(),
    var author : AppUser = AppUser(),
    var title: String = "",
    var description : String = "",
    var tags : MutableCollection<String> = mutableListOf(),
    var voteNumber: Int = 0,
    var postType: PostType = PostType.TEXT,
    var file: ByteArray = byteArrayOf(),
    var userVoteType : VoteType = VoteType.CLEAR,
    var isSavedByUser : Boolean = false
){
    fun initPersonalThreadPost(post : ThreadPost, user: AppUser) : PersonalThreadPost{
        return PersonalThreadPost().apply {
            this.postId  = post.postId
            this.author = post.author
            this.description = post.description
            this.isSavedByUser = user.savedPosts.contains(post)
            this.file = post.file
            //this.comments = post.comments.map {  }
            this.postTime = post.postTime
            this.userVoteType = if(user.upvotedPosts.contains(post)) VoteType.UPVOTED else if(user.downvotedPosts.contains(post)) VoteType.DOWNVOTED else VoteType.CLEAR
            this.postType = post.postType
            this.tags = post.tags
            this.title = post.title
            this.topicThread = post.topicThread
            this.voteNumber = post.voteNumber
        }
    }
}
