package hu.bme.aut.auth_service.domain

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.util.*

@Data
@AllArgsConstructor
@NoArgsConstructor
data class RefreshTokenRequest(
    val token : String = UUID.randomUUID().toString()
)