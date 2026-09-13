package hu.bme.aut.apigateway.filter

import hu.bme.aut.apigateway.util.JwtUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*

class AuthenticationFilterTest {

    private val secret = "iZuFy4mPoijNzJmMrfSlZZdz2JGVNLIkglKT6e9EMp0="
    private val otherSecret = "ZW3jSf0gYbUlMz1m8u1NvWl8Vt3y0hWq8rK9cPq7m0A="
    private val filter = AuthenticationFilter(RouteValidator(), JwtUtil(secret)).apply(AuthenticationFilter.Config())

    private class CapturingChain : GatewayFilterChain {
        var forwarded: ServerWebExchange? = null
        override fun filter(exchange: ServerWebExchange): Mono<Void> {
            forwarded = exchange
            return Mono.empty()
        }
    }

    private fun run(request: MockServerHttpRequest): Pair<MockServerWebExchange, CapturingChain> {
        val exchange = MockServerWebExchange.from(request)
        val chain = CapturingChain()
        filter.filter(exchange, chain).block()
        return exchange to chain
    }

    private fun token(
        key: String = secret,
        userId: Long? = 42,
        expiresInMillis: Long = 60_000
    ): String {
        val now = System.currentTimeMillis()
        val builder = Jwts.builder()
            .setSubject("alice")
            .setIssuedAt(Date(now))
            .setExpiration(Date(now + expiresInMillis))
        if (userId != null) builder.claim("userId", userId)
        return builder.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(key))).compact()
    }

    private fun securedRequest(authorization: String? = null) =
        MockServerHttpRequest.get("/api/thread/42/saved")
            .apply { if (authorization != null) header(HttpHeaders.AUTHORIZATION, authorization) }
            .header(AuthenticationFilter.USER_ID_HEADER, "1")
            .header(AuthenticationFilter.USER_NAME_HEADER, "mallory")
            .build()

    private fun assertRejected(request: MockServerHttpRequest) {
        val (exchange, chain) = run(request)
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        assertNull(chain.forwarded)
    }

    @Test
    fun validTokenForwardsIdentityHeadersAndReplacesSpoofedOnes() {
        val (_, chain) = run(securedRequest("Bearer ${token()}"))

        val headers = chain.forwarded!!.request.headers
        assertEquals(listOf("42"), headers[AuthenticationFilter.USER_ID_HEADER])
        assertEquals(listOf("alice"), headers[AuthenticationFilter.USER_NAME_HEADER])
    }

    @Test
    fun missingAuthorizationHeaderIsRejected() = assertRejected(securedRequest())

    @Test
    fun nonBearerAuthorizationHeaderIsRejected() = assertRejected(securedRequest("Basic dXNlcjpwYXNz"))

    @Test
    fun malformedTokenIsRejected() = assertRejected(securedRequest("Bearer not-a-jwt"))

    @Test
    fun tokenSignedWithAnotherKeyIsRejected() = assertRejected(securedRequest("Bearer ${token(key = otherSecret)}"))

    @Test
    fun expiredTokenIsRejected() = assertRejected(securedRequest("Bearer ${token(expiresInMillis = -60_000)}"))

    @Test
    fun tokenWithoutUserIdIsRejected() = assertRejected(securedRequest("Bearer ${token(userId = null)}"))

    @Test
    fun openEndpointPassesWithoutTokenButSpoofedHeadersAreStripped() {
        val request = MockServerHttpRequest.post("/api/auth/login")
            .header(AuthenticationFilter.USER_ID_HEADER, "1")
            .header(AuthenticationFilter.USER_NAME_HEADER, "mallory")
            .build()

        val (exchange, chain) = run(request)

        assertNull(exchange.response.statusCode)
        val headers = chain.forwarded!!.request.headers
        assertNull(headers[AuthenticationFilter.USER_ID_HEADER])
        assertNull(headers[AuthenticationFilter.USER_NAME_HEADER])
    }

    @Test
    fun pathContainingAnOpenEndpointIsStillSecured() =
        assertRejected(MockServerHttpRequest.get("/api/thread/1/api/auth/login").build())
}
