package hu.bme.aut.auth_service.domain

import com.fasterxml.jackson.annotation.JsonInclude

/** User as seen by other users: the email address is only included for the caller's own account. */
@JsonInclude(JsonInclude.Include.NON_NULL)
class UserResponse(
    val userId: Long,
    val userName: String,
    val email: String?,
    val profileImage: ByteArray
) {
    companion object {
        fun of(user: AppUser, viewerId: Long) =
            UserResponse(user.userId, user.userName, user.email.takeIf { user.userId == viewerId }, user.profileImage)
    }
}
