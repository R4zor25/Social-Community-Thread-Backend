package hu.bme.aut.projection

import hu.bme.aut.common.web.security.CurrentUser
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.Timestamp
import java.time.Clock

/** How another user appears in a response. */
data class UserRef(val id: Long, val username: String)

/** The service's read-only copy of users (id and username), kept current from user-events. */
class UserProjections(private val jdbc: NamedParameterJdbcTemplate, private val clock: Clock = Clock.systemUTC()) {

    fun upsert(userId: Long, username: String) {
        jdbc.update(
            """
            insert into user_projection (user_id, username, updated_at) values (:userId, :username, :now)
            on conflict (user_id) do update set username = excluded.username, updated_at = excluded.updated_at
            """,
            mapOf("userId" to userId, "username" to username, "now" to Timestamp.from(clock.instant()))
        )
    }

    /**
     * The caller may act before their UserRegistered event has arrived; their token already says who they are.
     * Call before storing anything that references the caller.
     */
    fun ensure(caller: CurrentUser) = upsert(caller.id, caller.username)

    fun exists(userId: Long): Boolean =
        jdbc.queryForObject("select exists (select 1 from user_projection where user_id = :userId)", mapOf("userId" to userId), Boolean::class.java) == true

    fun usernames(userIds: Collection<Long>): Map<Long, String> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            jdbc.query("select user_id, username from user_projection where user_id in (:ids)", mapOf("ids" to userIds.toSet())) { rs, _ ->
                rs.getLong("user_id") to rs.getString("username")
            }.toMap()
        }

    /** One query for all [userIds]. An id that is not projected yet still gets a reference, named "unknown". */
    fun refs(userIds: Collection<Long>): (Long) -> UserRef {
        val names = usernames(userIds)
        return { UserRef(it, names[it] ?: "unknown") }
    }
}
