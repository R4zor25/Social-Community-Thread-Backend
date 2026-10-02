package hu.bme.aut.common.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.StandardEnvironment
import org.springframework.mock.env.MockPropertySource

class ServiceDefaultsTest {

    private val readiness = "management.endpoint.health.group.readiness.include"

    @Test
    fun readinessIncludesTheDatabase() {
        val environment = StandardEnvironment()
        ServiceDefaults().postProcessEnvironment(environment, SpringApplication())

        assertThat(environment.getProperty(readiness)).isEqualTo("readinessState,db")
    }

    @Test
    fun aServiceCanOverrideTheDefault() {
        val environment = StandardEnvironment().apply { propertySources.addFirst(MockPropertySource().withProperty(readiness, "readinessState")) }
        ServiceDefaults().postProcessEnvironment(environment, SpringApplication())

        assertThat(environment.getProperty(readiness)).isEqualTo("readinessState")
    }
}
