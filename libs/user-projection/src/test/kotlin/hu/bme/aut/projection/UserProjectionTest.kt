package hu.bme.aut.projection

import hu.bme.aut.common.web.security.CurrentUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class UserProjectionTest : ProjectionTestSupport() {

    @Test
    fun anEventCreatesTheProjection() {
        send("101", registered(101, "alice"))

        assertThat(awaitValue { username(101) }).isEqualTo("alice")
    }

    /** A redelivered event is applied again without error; a later event with the same key proves both were handled. */
    @Test
    fun theSameEventTwiceIsHarmless() {
        val event = registered(102, "bob")
        send("102", event)
        send("102", event)
        send("102", registered(102, "bob-later"))

        assertThat(awaitValue { username(102)?.takeIf { it == "bob-later" } }).isEqualTo("bob-later")
        assertThat(deadLetters()).noneMatch { it == event }
    }

    @Test
    fun eventAfterTokenUpsertKeepsOneRow() {
        projections.ensure(CurrentUser(104, "carol"))
        val upsertedAt = jdbc.queryForObject("select updated_at from user_projection where user_id = 104", Instant::class.java)!!

        send("104", registered(104, "carol"))

        val appliedAt = awaitValue { jdbc.queryForObject("select updated_at from user_projection where user_id = 104", Instant::class.java)!!.takeIf { it.isAfter(upsertedAt) } }
        assertThat(appliedAt).describedAs("the event was applied on top of the token upsert").isNotNull()
        assertThat(username(104)).isEqualTo("carol")
    }

    @Test
    fun unknownEventTypesAreSkippedWithoutDeadLettering() {
        val unknown = """{"eventId":"0f0e0d0c-0b0a-0908-0706-050403020100","type":"UserRenamed","version":1,"occurredAt":"2026-01-01T00:00:00Z","payload":{"userId":105,"username":"x"}}"""
        send("105", unknown)
        send("105", registered(105, "dave"))

        assertThat(awaitValue { username(105) }).isEqualTo("dave")
        assertThat(deadLetters()).noneMatch { it.contains("0f0e0d0c-0b0a-0908-0706-050403020100") }
    }

    @Test
    fun usernamesAreLoadedForKnownIdsOnly() {
        projections.ensure(CurrentUser(106, "erin"))
        projections.ensure(CurrentUser(107, "frank"))

        assertThat(projections.usernames(listOf(106, 107, 999))).isEqualTo(mapOf(106L to "erin", 107L to "frank"))
        assertThat(projections.usernames(emptyList())).isEmpty()
    }

    @Test
    fun existsOnlyForProjectedUsers() {
        projections.ensure(CurrentUser(108, "grace"))

        assertThat(projections.exists(108)).isTrue()
        assertThat(projections.exists(999)).isFalse()
    }
}
