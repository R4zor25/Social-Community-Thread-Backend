package hu.bme.aut.thread

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ThreadServiceApplication

fun main(args: Array<String>) {
    runApplication<ThreadServiceApplication>(*args)
}
