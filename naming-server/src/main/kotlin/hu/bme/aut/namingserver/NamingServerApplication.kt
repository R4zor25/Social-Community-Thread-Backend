package hu.bme.aut.namingserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

@EnableEurekaServer
@SpringBootApplication
open class NamingServerApplication

fun main(args: Array<String>) {
	runApplication<NamingServerApplication>(*args)
}
