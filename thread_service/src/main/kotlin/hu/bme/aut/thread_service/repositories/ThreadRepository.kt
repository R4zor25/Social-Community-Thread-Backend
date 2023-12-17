package hu.bme.aut.thread_service.repositories

import hu.bme.aut.thread_service.models.entities.TopicThread
import org.springframework.data.jpa.repository.JpaRepository

interface ThreadRepository : JpaRepository<TopicThread, Long> {
}