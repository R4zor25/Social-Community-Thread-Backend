package hu.bme.aut.chat

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The context only starts if Hibernate's schema validation accepts the Flyway-migrated schema. */
class MigrationTest : IntegrationTest() {

    @Test
    fun schemaIsMigrated() {
        val tables = jdbc.queryForList("select table_name from information_schema.tables where table_schema = 'public'", String::class.java)
        assertThat(tables).contains("user_projection", "conversations", "conversation_images", "conversation_participants", "messages")
    }

    @Test
    fun deletingAConversationRemovesItsContentButNoUsers() {
        jdbc.update("insert into conversations (name, creator_id, created_at, last_message_at) values ('c', 1, now(), now())")
        jdbc.update("insert into conversation_participants values (1, 1, now()), (1, 2, now())")
        jdbc.update("insert into messages (conversation_id, author_id, body, sent_at) values (1, 2, 'hi', now())")

        jdbc.update("delete from conversations")

        assertThat(count("conversation_participants")).isZero()
        assertThat(count("messages")).isZero()
        assertThat(count("user_projection")).isEqualTo(3)
    }
}
