package hu.bme.aut.dipterv

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableConfigServer
@ComponentScan("friend-service")
open class ConfigServiceApplication

fun main(args: Array<String>) {
	runApplication<ConfigServiceApplication>(*args)
}
