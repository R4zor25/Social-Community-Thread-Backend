package hu.bme.aut.thread_service

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableAutoConfiguration
@EnableDiscoveryClient
open class ThreadServiceApplication

fun main(args: Array<String>) {
	runApplication<ThreadServiceApplication>(*args)
}
