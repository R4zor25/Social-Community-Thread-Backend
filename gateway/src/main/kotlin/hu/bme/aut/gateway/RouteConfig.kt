package hu.bme.aut.gateway

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.PredicateSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize

/** Base URLs of the services, e.g. GATEWAY_SERVICES_AUTH=http://auth-service:8080. */
@ConfigurationProperties("gateway.services")
data class ServiceUrls(val auth: String, val thread: String, val friend: String, val chat: String)

/** Each path prefix belongs to exactly one service; anything else is not routed. */
@Configuration(proxyBeanMethods = false)
class RouteConfig {

    @Bean
    fun routes(builder: RouteLocatorBuilder, services: ServiceUrls): RouteLocator = builder.routes()
        .route("auth") { it.to(services.auth, "/api/v2/auth/**", "/api/v2/users/**") }
        .route("thread") { it.to(services.thread, "/api/v2/threads/**", "/api/v2/posts/**", "/api/v2/comments/**", "/api/v2/feed/**") }
        .route("friend") { it.to(services.friend, "/api/v2/friends/**", "/api/v2/friend-requests/**") }
        .route("chat") { it.to(services.chat, "/api/v2/conversations/**") }
        .build()

    private fun PredicateSpec.to(uri: String, vararg paths: String) =
        path(*paths).filters { it.setRequestSize(MAX_REQUEST_SIZE) }.uri(uri)

    companion object {
        /** Uploads are limited to 5 MB by the services; this leaves room for the rest of the request. */
        val MAX_REQUEST_SIZE: DataSize = DataSize.ofMegabytes(6)
    }
}
