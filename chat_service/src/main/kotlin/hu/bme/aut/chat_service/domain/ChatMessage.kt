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
class ChatMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var author: AppUser = AppUser(),

    var messageText : String = "",
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    var sentDate: Date = Date()
)