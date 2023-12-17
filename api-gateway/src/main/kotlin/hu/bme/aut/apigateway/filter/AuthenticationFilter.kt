package hu.bme.aut.apigateway.filter

import hu.bme.aut.apigateway.client.AuthClient
import jakarta.ws.rs.core.HttpHeaders
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono





@Component
class AuthenticationFilter(
    private val validator: RouteValidator,
    @Lazy
    private val authClient: AuthClient
) : AbstractGatewayFilterFactory<AuthenticationFilter.Config>(Config::class.java) {

    val webClient = WebClient.builder()
        .baseUrl("http://localhost:49018")
        .defaultCookie("cookie-name", "cookie-value")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    //    @Autowired
    //    private RestTemplate template;
    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            if (validator.isSecured.test(exchange.request)) {
                //header contains token or not
                if (!exchange.request.headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw RuntimeException("missing authorization header")
                }
                var authHeader = exchange.request.headers[HttpHeaders.AUTHORIZATION]!![0]
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7)
                }
                try {
                    val authResponse: Mono<String> = webClient.post()
                        .uri("/api/auth/validate")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(Mono.just(authHeader), String::class.java)
                        .retrieve()
                        .bodyToMono(String::class.java)
                } catch (e: Exception) {
                    throw RuntimeException(e.localizedMessage)
                }
            }
            chain.filter(exchange)
        }
    }

    class Config
}