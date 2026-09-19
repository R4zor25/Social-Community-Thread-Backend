package hu.bme.aut.events

object UserEvents {
    const val TOPIC = "user-events"
    const val DEAD_LETTER_TOPIC = "user-events.DLT"
    const val PARTITIONS = 3
}

data class UserRegistered(val userId: Long, val username: String) {
    companion object {
        const val TYPE = "UserRegistered"
        const val VERSION = 1
    }
}
