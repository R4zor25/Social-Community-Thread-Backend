package hu.bme.aut.thread_service.models

import hu.bme.aut.thread_service.models.entities.AppUser
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@Data
@AllArgsConstructor
@NoArgsConstructor
data class PersonalTopicThread(
    var topicThreadId: Long? = 0,
    var name: String  = "",
    var threadImage : ByteArray = byteArrayOf(),
    var description: String = "",
    var threadposts: MutableCollection<ThreadPost> = mutableListOf(),
    var isFollowedByUser : Boolean = false,
)
{
    fun initPersonalTopicThread(thread: TopicThread, user: AppUser) : PersonalTopicThread{
        return PersonalTopicThread().apply {
            this.topicThreadId = thread.topicThreadId
            this.name = thread.name
            this.description = thread.description
            this.threadImage = thread.threadImage
            this.isFollowedByUser = user.followedThreads.contains(thread)
            this.threadposts = thread.threadposts
        }
    }
}