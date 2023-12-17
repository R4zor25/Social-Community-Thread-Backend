package hu.bme.aut.thread_service.repositories

import hu.bme.aut.thread_service.models.entities.ThreadPost
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<ThreadPost, Long> {
}