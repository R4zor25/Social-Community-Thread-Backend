package hu.bme.aut.friend_service.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class AppUser (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var userId: Long = 0,

    var userName: String = "",

    var email: String = "",

    @JsonIgnore
    var password: String = "",

    @Lob
    @Column(length = Integer.MAX_VALUE)
    var profileImage : ByteArray = byteArrayOf(),

    @ManyToMany(cascade = [CascadeType.ALL])
    @JsonIgnore
    var followedThreads : MutableCollection<TopicThread> = mutableListOf(),

    @ManyToMany(cascade = [CascadeType.ALL])
    @JsonIgnore
    var savedPosts : MutableCollection<ThreadPost> = mutableListOf(),

    @ManyToMany(cascade = [CascadeType.ALL])
    @JsonIgnore
    var upvotedPosts : MutableCollection<ThreadPost> = mutableListOf(),

    @ManyToMany(cascade = [CascadeType.ALL])
    @JsonIgnore
    var downvotedPosts : MutableCollection<ThreadPost> = mutableListOf(),

    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JsonIgnore
    var upvotedComments : MutableCollection<CommentModel> = mutableListOf(),

    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JsonIgnore
    var downvotedComments : MutableCollection<CommentModel> = mutableListOf(),

    @ManyToMany(cascade = [CascadeType.ALL])
    @JsonIgnore
    var friends : MutableCollection<AppUser> = mutableListOf(),

    @ManyToMany(cascade = [CascadeType.ALL])
    @JsonIgnore
    var outgoingFriendRequests : MutableCollection<AppUser> = mutableListOf(),

    @ManyToMany( cascade = [CascadeType.ALL])
    @JsonIgnore
    var incomingFriendRequests : MutableCollection<AppUser> = mutableListOf()
)