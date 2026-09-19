package hu.bme.aut.events

import java.time.Instant
import java.util.UUID

/** Wire format of every event; `type` and `version` let consumers pick the payload class. */
data class EventEnvelope<T>(
    val eventId: UUID,
    val type: String,
    val version: Int,
    val occurredAt: Instant,
    val payload: T
)
