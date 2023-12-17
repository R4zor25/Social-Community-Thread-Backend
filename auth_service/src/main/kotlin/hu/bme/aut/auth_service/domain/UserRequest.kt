package hu.bme.aut.auth_service.domain

import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

@NoArgsConstructor
@AllArgsConstructor
@Data
class UserRequest(
    val username: String,
    val password: String,
    val email: String
)