package hu.bme.aut.auth_service.domain

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@Data
@AllArgsConstructor
@NoArgsConstructor
data class AuthRequest(
    val username : String,
    val password : String
) {
}