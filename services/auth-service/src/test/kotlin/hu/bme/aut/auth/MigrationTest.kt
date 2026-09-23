package hu.bme.aut.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The context only starts if Hibernate's schema validation accepts the Flyway-migrated schema. */
class MigrationTest : IntegrationTest() {

    @Test
    fun schemaIsMigratedAndMatchesTheEntities() {
        val tables = jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'", String::class.java
        )

        assertThat(tables).contains("users", "user_avatars", "sessions", "refresh_tokens", "outbox", "flyway_schema_history")
    }

    @Test
    fun usernamesAndEmailsAreUniqueIgnoringCase() {
        jdbc.update("insert into users (username, email, password_hash, created_at) values ('alice', 'a@x.hu', 'h', now())")

        assertThat(runCatching {
            jdbc.update("insert into users (username, email, password_hash, created_at) values ('ALICE', 'other@x.hu', 'h', now())")
        }.isFailure).isTrue()
        assertThat(runCatching {
            jdbc.update("insert into users (username, email, password_hash, created_at) values ('bob', 'A@X.HU', 'h', now())")
        }.isFailure).isTrue()
    }
}
