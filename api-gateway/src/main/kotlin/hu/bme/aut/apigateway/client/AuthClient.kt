package hu.bme.aut.apigateway.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "auth-service")
interface AuthClient {

    @GetMapping("/api/auth/validate")
    fun validateToken(@RequestParam("Token") token: String): String
}