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

    fun typeOf(json: String): String = mapper.readTree(json).get("type").asString()

    fun <T : Any> read(json: String, payloadType: Class<T>): EventEnvelope<T> =
        mapper.readValue(json, mapper.typeFactory.constructParametricType(EventEnvelope::class.java, payloadType))
}
