package hu.bme.aut.gateway

import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange

/**
 * Sends the address the gateway was connected from as the only X-Forwarded-For value. auth-service throttles logins
 * per client address, so it must neither see the gateway's own address nor a value the client wrote itself.
 * The gateway is the edge here; behind another proxy, this would have to come from that proxy instead.
 */
@Component
class ClientAddressFilter : HttpHeadersFilter, Ordered {

    override fun filter(input: HttpHeaders, exchange: ServerWebExchange): HttpHeaders {
        val address = exchange.request.remoteAddress?.address?.hostAddress ?: return input
        return HttpHeaders(input).apply { set(X_FORWARDED_FOR, address) }
    }

    // Runs after the built-in filter that strips forwarded headers sent by the client.
    override fun getOrder() = Ordered.LOWEST_PRECEDENCE

    companion object {
        const val X_FORWARDED_FOR = "X-Forwarded-For"
    }
}
