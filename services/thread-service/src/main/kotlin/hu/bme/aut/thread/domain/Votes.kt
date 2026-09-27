package hu.bme.aut.thread.domain

enum class VoteDirection(val value: Int) {
    UP(1), DOWN(-1);

    companion object {
        fun of(value: Int) = entries.single { it.value == value }
    }
}

/** How much a score changes when a user's vote goes from [previous] to [next]; null means no vote. */
fun voteDelta(previous: VoteDirection?, next: VoteDirection?): Int = (next?.value ?: 0) - (previous?.value ?: 0)
