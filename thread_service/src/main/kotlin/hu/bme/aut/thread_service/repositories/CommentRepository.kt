package hu.bme.aut.thread_service.repositories

import hu.bme.aut.thread_service.models.entities.CommentModel
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<CommentModel, Long>{
}