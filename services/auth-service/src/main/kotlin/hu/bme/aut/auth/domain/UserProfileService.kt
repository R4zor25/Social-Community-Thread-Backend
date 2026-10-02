package hu.bme.aut.auth.domain

import hu.bme.aut.auth.persistence.AvatarRepository
import hu.bme.aut.auth.persistence.UserRepository
import hu.bme.aut.common.web.error.NotFoundException
import hu.bme.aut.common.web.upload.Upload
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(private val users: UserRepository, private val avatars: AvatarRepository) {

    @Transactional(readOnly = true)
    fun find(id: Long): User = users.findByIdOrNull(id) ?: throw NotFoundException("User does not exist")

    @Transactional(readOnly = true)
    fun search(usernamePrefix: String, pageable: Pageable): Page<User> =
        users.findByUsernameStartingWithIgnoreCaseOrderByUsernameAsc(usernamePrefix, pageable)

    @Transactional
    fun setAvatar(userId: Long, upload: Upload) {
        find(userId)
        val avatar = avatars.findByIdOrNull(userId)
        if (avatar == null) {
            avatars.save(Avatar(userId, upload.content, upload.contentType))
        } else {
            avatar.content = upload.content
            avatar.contentType = upload.contentType
        }
    }

    @Transactional(readOnly = true)
    fun avatar(userId: Long): Avatar = avatars.findByIdOrNull(userId) ?: throw NotFoundException("User has no avatar")
}
