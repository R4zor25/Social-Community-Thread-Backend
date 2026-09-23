package hu.bme.aut.auth.domain

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** The RSA signing key pair from PEM (PKCS#8 private, X.509 public); the key id is the key's SHA-256 thumbprint. */
class KeyProvider(privateKeyPem: String, publicKeyPem: String) {

    val rsaKey: RSAKey

    init {
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(der(publicKeyPem))) as RSAPublicKey
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(der(privateKeyPem))) as RSAPrivateKey
        val thumbprint = RSAKey.Builder(publicKey).build().computeThumbprint().toString()
        rsaKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(thumbprint)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .build()
    }

    /** Only the public half, safe to publish. */
    val publicJwkSet: JWKSet get() = JWKSet(rsaKey.toPublicJWK())

    val publicKey: RSAPublicKey get() = rsaKey.toRSAPublicKey()

    companion object {
        fun fromFiles(privateKeyPath: Path, publicKeyPath: Path) =
            KeyProvider(Files.readString(privateKeyPath), Files.readString(publicKeyPath))

        private fun der(pem: String): ByteArray =
            Base64.getMimeDecoder().decode(pem.lines().filterNot { it.startsWith("-----") }.joinToString(""))
    }
}
