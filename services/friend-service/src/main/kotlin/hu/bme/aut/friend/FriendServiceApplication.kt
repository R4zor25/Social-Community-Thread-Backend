package hu.bme.aut.friend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FriendServiceApplication

fun main(args: Array<String>) {
    runApplication<FriendServiceApplication>(*args)
}
