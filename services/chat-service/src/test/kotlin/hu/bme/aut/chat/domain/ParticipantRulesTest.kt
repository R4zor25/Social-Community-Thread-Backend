package hu.bme.aut.chat.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ParticipantRulesTest {

    @ParameterizedTest(name = "caller {0} removing {1} (creator {2}) -> {3}")
    @CsvSource(
        "2, 2, 1, true",
        "1, 1, 1, true",
        "1, 2, 1, true",
        "2, 3, 1, false",
        "2, 1, 1, false"
    )
    fun anyoneMayLeaveOnlyTheCreatorMayRemoveOthers(caller: Long, target: Long, creator: Long, allowed: Boolean) {
        assertThat(mayRemove(callerId = caller, targetId = target, creatorId = creator)).isEqualTo(allowed)
    }
}
