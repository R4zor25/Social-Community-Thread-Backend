package hu.bme.aut.friend.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FriendshipPairTest {

    @Test
    fun aPairIsStoredWithTheLowerIdFirstWhicheverWayItIsGiven() {
        assertThat(FriendshipId.of(7, 3)).isEqualTo(FriendshipId(3, 7))
        assertThat(FriendshipId.of(3, 7)).isEqualTo(FriendshipId(3, 7))
    }

    @Test
    fun theOtherUserIsTheOneThatIsNotMe() {
        assertThat(FriendshipId(3, 7).other(3)).isEqualTo(7)
        assertThat(FriendshipId(3, 7).other(7)).isEqualTo(3)
    }
}
