package hu.bme.aut.auth_service.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "TopicThreads")
class TopicThread(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var topicThreadId: Long? = 0,
    var name: String  = "",
    @Lob
    @Column(length = Integer.MAX_VALUE)
    var threadImage : ByteArray = byteArrayOf(),
    var description: String = "",

    @OneToMany(mappedBy = "topicThread", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JsonManagedReference
    @JsonIgnore
    var threadposts: MutableCollection<ThreadPost> = mutableListOf(),
)