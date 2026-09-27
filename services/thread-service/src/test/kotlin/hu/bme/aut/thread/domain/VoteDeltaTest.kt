package hu.bme.aut.thread.domain

import hu.bme.aut.thread.domain.VoteDirection.DOWN
import hu.bme.aut.thread.domain.VoteDirection.UP
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VoteDeltaTest {

    private fun direction(value: String) = if (value == "NONE") null else VoteDirection.valueOf(value)

    @ParameterizedTest(name = "{0} -> {1} changes the score by {2}")
    @CsvSource(
        "NONE, UP, 1",
        "NONE, DOWN, -1",
        "UP, DOWN, -2",
        "DOWN, UP, 2",
        "UP, NONE, -1",
        "DOWN, NONE, 1",
        "UP, UP, 0",
        "DOWN, DOWN, 0",
        "NONE, NONE, 0"
    )
    fun scoreChangeIsTheDifferenceOfTheVotes(previous: String, next: String, delta: Int) {
        assertThat(voteDelta(direction(previous), direction(next))).isEqualTo(delta)
    }

    @org.junit.jupiter.api.Test
    fun directionsMapToPlusAndMinusOne() {
        assertThat(UP.value).isEqualTo(1)
        assertThat(DOWN.value).isEqualTo(-1)
        assertThat(VoteDirection.of(1)).isEqualTo(UP)
        assertThat(VoteDirection.of(-1)).isEqualTo(DOWN)
    }
}
