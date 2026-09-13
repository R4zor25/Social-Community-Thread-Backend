package hu.bme.aut.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient


@SpringBootApplication
@EnableDiscoveryClient
open class ApiGatewayApplication{

}

fun main(args: Array<String>) {
	runApplication<ApiGatewayApplication>(*args)
}


