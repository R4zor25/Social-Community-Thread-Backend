package hu.bme.aut.common.web.upload

import hu.bme.aut.common.web.error.ApiException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.io.InputStream

const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024

/** A binary request body of at most 5 MB, with only type/subtype of its media type (no charset or other parameters). */
class Upload(val content: ByteArray, val contentType: String)

/** Null when [input] holds more than [limit] bytes. Reads at most one byte past the limit, so a body without Content-Length cannot fill the heap. */
fun readAtMost(input: InputStream, limit: Int): ByteArray? = input.readNBytes(limit + 1).takeIf { it.size <= limit }

class UploadArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == Upload::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Upload {
        val request = requireNotNull(webRequest.getNativeRequest(HttpServletRequest::class.java))
        if (request.contentLengthLong > MAX_UPLOAD_BYTES) throw tooLarge()
        val content = readAtMost(request.inputStream, MAX_UPLOAD_BYTES) ?: throw tooLarge()
        val type = MediaType.parseMediaType(requireNotNull(request.contentType) { "The endpoint must declare what it consumes" })
        return Upload(content, "${type.type}/${type.subtype}")
    }

    private fun tooLarge() = ApiException(HttpStatus.CONTENT_TOO_LARGE, "Uploads are limited to 5 MB")
}
