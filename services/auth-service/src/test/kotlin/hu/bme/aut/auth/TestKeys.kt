package hu.bme.aut.auth

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64

/** Generates throwaway RSA keys for tests; no key material is committed. */
object TestKeys {
    fun generate(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    fun privatePem(keys: KeyPair) = pem("PRIVATE KEY", keys.private.encoded)
    fun publicPem(keys: KeyPair) = pem("PUBLIC KEY", keys.public.encoded)

    /** The key pair used by every Spring test context, written to temporary PEM files once. */
    val shared: KeyPair by lazy { generate() }
    val sharedPrivateKeyFile: Path by lazy { write("private", privatePem(shared)) }
    val sharedPublicKeyFile: Path by lazy { write("public", publicPem(shared)) }

    private fun write(name: String, content: String): Path =
        Files.createTempFile("auth-test-$name", ".pem").also { Files.writeString(it, content); it.toFile().deleteOnExit() }

    private fun pem(type: String, der: ByteArray) =
        "-----BEGIN $type-----\n" + Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der) + "\n-----END $type-----\n"
}
