package hu.bme.aut.auth.api

import hu.bme.aut.auth.domain.KeyProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Public signing keys for the gateway and the services. Internal: the gateway does not route it. */
@RestController
class JwksController(private val keyProvider: KeyProvider) {

    @GetMapping("/.well-known/jwks.json")
    fun jwks(): Map<String, Any> = keyProvider.publicJwkSet.toJSONObject()
}
