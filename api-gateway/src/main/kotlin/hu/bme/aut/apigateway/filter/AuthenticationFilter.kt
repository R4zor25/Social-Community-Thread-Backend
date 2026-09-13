package hu.bme.aut.apigateway.filter

import hu.bme.aut.apigateway.util.JwtUtil
import io.jsonwebtoken.JwtException
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class AuthenticationFilter(
    private val validator: RouteValidator,
    private val jwtUtil: JwtUtil
) : AbstractGatewayFilterFactory<AuthenticationFilter.Config>(Config::class.java) {

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            // Identity headers are only ever set by the gateway, never passed through from the client.
            val request = exchange.request.mutate()
                .headers { it.remove(USER_ID_HEADER); it.remove(USER_NAME_HEADER) }
                .build()

            if (!validator.isSecured.test(request)) {
                return@GatewayFilter chain.filter(exchange.mutate().request(request).build())
            }

            val identity = authenticate(request) ?: return@GatewayFilter unauthorized(exchange)
            val authenticatedRequest = request.mutate()
                .header(USER_ID_HEADER, identity.first.toString())
                .header(USER_NAME_HEADER, identity.second)
                .build()
            chain.filter(exchange.mutate().request(authenticatedRequest).build())
        }
    }

    private fun authenticate(request: ServerHttpRequest): Pair<Long, String>? {
        val header = request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return null
        if (!header.startsWith(BEARER_PREFIX)) return null
        val claims = try {
            jwtUtil.parseClaims(header.substring(BEARER_PREFIX.length))
        } catch (e: JwtException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }
        val userId = (claims["userId"] as? Number)?.toLong() ?: return null
        val userName = claims.subject ?: return null
        return userId to userName
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }

    class Config

    companion object {
        const val USER_ID_HEADER = "X-User-Id"
        const val USER_NAME_HEADER = "X-User-Name"
        private const val BEARER_PREFIX = "Bearer "
    }
}
