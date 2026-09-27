package hu.bme.aut.thread

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The context only starts if Hibernate's schema validation accepts the Flyway-migrated schema. */
class MigrationTest : IntegrationTest() {

    @Test
    fun schemaIsMigratedAndMatchesTheEntities() {
        val tables = jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'", String::class.java
        )

        assertThat(tables).contains(
            "user_projection", "threads", "thread_images", "thread_followers", "posts", "post_attachments",
            "post_votes", "saved_posts", "comments", "comment_votes"
        )
    }

    @Test
    fun deletingAThreadRemovesItsContentButNoUsers() {
        jdbc.update("insert into user_projection values (1, 'alice', now()), (2, 'bob', now())")
        jdbc.update("insert into threads (name, description, creator_id, created_at) values ('t', '', 1, now())")
        jdbc.update("insert into posts (thread_id, author_id, title, body, tags, created_at) values (1, 2, 'p', '', '{}', now())")
        jdbc.update("insert into comments (post_id, author_id, body, created_at) values (1, 1, 'c', now())")
        jdbc.update("insert into post_votes values (1, 1, 1)")
        jdbc.update("insert into saved_posts values (1, 1, now())")
        jdbc.update("insert into thread_followers values (1, 2, now())")

        jdbc.update("delete from threads where id = 1")

        listOf("posts", "comments", "post_votes", "saved_posts", "thread_followers").forEach {
            assertThat(jdbc.queryForObject("select count(*) from $it", Long::class.java)).describedAs(it).isZero()
        }
        assertThat(jdbc.queryForObject("select count(*) from user_projection", Long::class.java)).isEqualTo(2)
    }

    @Test
    fun aVoteMustBeUpOrDown() {
        jdbc.update("insert into user_projection values (1, 'alice', now())")
        jdbc.update("insert into threads (name, description, creator_id, created_at) values ('t', '', 1, now())")
        jdbc.update("insert into posts (thread_id, author_id, title, body, tags, created_at) values (1, 1, 'p', '', '{}', now())")

        assertThat(runCatching { jdbc.update("insert into post_votes values (1, 1, 2)") }.isFailure).isTrue()
    }
}
