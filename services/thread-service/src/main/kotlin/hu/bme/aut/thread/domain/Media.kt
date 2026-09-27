package hu.bme.aut.thread.domain

import hu.bme.aut.common.web.error.ApiException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024

fun requireUploadSize(content: ByteArray) {
    if (content.size > MAX_UPLOAD_BYTES) throw ApiException(HttpStatus.CONTENT_TOO_LARGE, "Uploads are limited to 5 MB")
}

/** Keeps only type/subtype of a Content-Type header, so parameters a client sends (e.g. charset) are not stored. */
fun mediaTypeOf(contentType: String): String = MediaType.parseMediaType(contentType).let { "${it.type}/${it.subtype}" }
