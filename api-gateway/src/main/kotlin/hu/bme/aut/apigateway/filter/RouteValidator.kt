package hu.bme.aut.apigateway.filter


import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import java.util.function.Predicate


@Component
class RouteValidator {
    var isSecured: Predicate<ServerHttpRequest> = Predicate<ServerHttpRequest> { request ->
        request.path.value() !in openApiEndpoints
    }

    companion object {
        // Exact matches only: anything else, including encoded or suffixed variants, requires a token.
        val openApiEndpoints = setOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refreshToken"
        )
    }
}
