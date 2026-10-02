package hu.bme.aut.common.web

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/** Settings every service shares; added with the lowest precedence, so a service's own configuration wins. */
class ServiceDefaults : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        environment.propertySources.addLast(
            MapPropertySource(
                "common-web-defaults",
                // Not ready without the database. Boot has no Kafka health indicator; the consumers reconnect on their own.
                mapOf("management.endpoint.health.group.readiness.include" to "readinessState,db")
            )
        )
    }
}
