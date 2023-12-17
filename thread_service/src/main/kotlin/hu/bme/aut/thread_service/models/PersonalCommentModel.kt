package hu.bme.aut.thread_service.models

import com.fasterxml.jackson.annotation.JsonFormat
import hu.bme.aut.thread_service.models.entities.AppUser
import hu.bme.aut.thread_service.models.entities.CommentModel
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.VoteType
import java.util.*

data class PersonalCommentModel(
    var id: Long = 0L,
    var threadPost : ThreadPost = ThreadPost(),
    var author : AppUser = AppUser(),
    var voteNumber: Int = 0,
    var commentText: String = "",
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    var commentTime: Date = Date(),
    var voteType: VoteType = VoteType.CLEAR,

)
{
    fun initPersonalCommentModel(commentModel: CommentModel, voteType: VoteType) {
        this.id = commentModel.id!!
        this.threadPost = commentModel.threadPost
        this.author = commentModel.author
        this.voteNumber = commentModel.voteNumber
        this.commentTime = commentModel.commentTime
        this.voteType = voteType
        this.commentText = commentModel.commentText
        this.voteType = voteType
    }
}