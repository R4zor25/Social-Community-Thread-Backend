package hu.bme.aut.chat_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
open class ChatServiceApplication

fun main(args: Array<String>) {
	runApplication<ChatServiceApplication>(*args)
}
