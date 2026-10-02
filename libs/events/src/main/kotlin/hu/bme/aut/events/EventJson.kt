package hu.bme.aut.events

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object EventJson {
    private val mapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    fun write(envelope: EventEnvelope<*>): String = mapper.writeValueAsString(envelope)

    /** Throws IllegalArgumentException when the record has no type, so it is treated as malformed. */
    fun typeOf(json: String): String = requireNotNull(mapper.readTree(json).get("type")?.asString()) { "Event has no type" }

    fun <T : Any> read(json: String, payloadType: Class<T>): EventEnvelope<T> =
        mapper.readValue(json, mapper.typeFactory.constructParametricType(EventEnvelope::class.java, payloadType))
}
