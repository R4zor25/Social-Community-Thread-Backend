package hu.bme.aut.chat_service.domain

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.util.*

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class ChatConversation(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id : Long? = 0,

    var conversationName: String = "",
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    var creationDate: Date = Date(),

    @ManyToOne
    @JoinColumn(name = "user_id")
    var chatCreator : AppUser = AppUser(),

    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var chatParticipants : MutableCollection<AppUser> = mutableListOf(),

    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var messageList : MutableCollection<ChatMessage> = mutableListOf(),

    @Lob
    var conversationImage: ByteArray = byteArrayOf(),
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    var lastMessageDate: Date = Date()
)