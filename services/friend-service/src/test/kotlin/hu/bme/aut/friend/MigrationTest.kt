package hu.bme.aut.friend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The context only starts if Hibernate's schema validation accepts the Flyway-migrated schema. */
class MigrationTest : IntegrationTest() {

    private fun fails(sql: String) = runCatching { jdbc.update(sql) }.isFailure

    @Test
    fun schemaIsMigrated() {
        val tables = jdbc.queryForList("select table_name from information_schema.tables where table_schema = 'public'", String::class.java)
        assertThat(tables).contains("user_projection", "friend_requests", "friendships")
    }

    @Test
    fun aRequestToYourselfIsRejected() {
        assertThat(fails("insert into friend_requests (sender_id, recipient_id, created_at) values (1, 1, now())")).isTrue()
    }

    @Test
    fun onlyOneRequestPerPairWhicheverDirection() {
        jdbc.update("insert into friend_requests (sender_id, recipient_id, created_at) values (1, 2, now())")

        assertThat(fails("insert into friend_requests (sender_id, recipient_id, created_at) values (1, 2, now())")).isTrue()
        assertThat(fails("insert into friend_requests (sender_id, recipient_id, created_at) values (2, 1, now())")).isTrue()
    }

    @Test
    fun aFriendshipIsStoredLowerIdFirst() {
        assertThat(fails("insert into friendships (user_low, user_high, created_at) values (2, 1, now())")).isTrue()
        assertThat(fails("insert into friendships (user_low, user_high, created_at) values (1, 1, now())")).isTrue()
    }
}
