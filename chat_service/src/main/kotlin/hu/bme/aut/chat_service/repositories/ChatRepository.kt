package hu.bme.aut.chat_service.repositories

import hu.bme.aut.chat_service.domain.ChatConversation
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepository : JpaRepository<ChatConversation, Long> {
}