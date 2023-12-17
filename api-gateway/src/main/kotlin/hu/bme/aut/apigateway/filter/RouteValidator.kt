package hu.bme.aut.apigateway.filter


import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import java.util.function.Predicate


@Component
class RouteValidator {
    var isSecured: Predicate<ServerHttpRequest> = Predicate<ServerHttpRequest> { request ->
        openApiEndpoints
            .stream()
            .noneMatch { uri: String? -> request.uri.getPath().contains(uri.toString()) }
    }

    companion object {
        val openApiEndpoints = listOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refreshToken",
            "/eureka"
        )
    }
}