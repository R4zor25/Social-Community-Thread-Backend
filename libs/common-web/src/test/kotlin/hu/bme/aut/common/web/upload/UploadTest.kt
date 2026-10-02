package hu.bme.aut.common.web.upload

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.InputStream

class UploadTest {

    /** Never ends, like a chunked body that is not stopped: reading must still stop after the limit. */
    private class EndlessStream : InputStream() {
        var served = 0L
        override fun read(): Int = 0.also { served++ }
        override fun read(b: ByteArray, off: Int, len: Int): Int = len.also { served += it }
    }

    @Test
    fun readingStopsJustAfterTheLimit() {
        val input = EndlessStream()

        assertThat(readAtMost(input, 1024)).isNull()
        assertThat(input.served).isLessThanOrEqualTo(1024 + 8192L)
    }

    @Test
    fun bodiesWithinTheLimitAreReturnedWhole() {
        assertThat(readAtMost(ByteArray(1024) { 7 }.inputStream(), 1024)).hasSize(1024).containsOnly(7)
    }
}
