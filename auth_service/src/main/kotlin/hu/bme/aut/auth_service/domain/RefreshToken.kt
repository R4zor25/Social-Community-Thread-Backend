package hu.bme.aut.auth_service.domain

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor
import java.time.Instant

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var token: String = "",
    var expiryDate: Instant = Instant.MIN,
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    var user: AppUser = AppUser()
)