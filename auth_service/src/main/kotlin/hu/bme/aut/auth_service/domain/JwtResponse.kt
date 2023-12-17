package hu.bme.aut.auth_service.domain

import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
data class JwtResponse(
    var accessToken : String = "",
    var token : String = "",
    var user : AppUser = AppUser()
)