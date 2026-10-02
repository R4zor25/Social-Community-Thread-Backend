package hu.bme.aut.common.web.paging

import org.springframework.data.domain.Page

data class PageResponse<T>(val items: List<T>, val page: Int, val size: Int, val totalItems: Long, val totalPages: Int)

fun <T : Any, R> Page<T>.toResponse(mapper: (T) -> R) =
    PageResponse(content.map(mapper), number, size, totalElements, totalPages)

/** Maps the whole page in one call, so the mapper can load what it needs for all items with one query each. */
fun <T : Any, R> Page<T>.toBatchResponse(mapper: (List<T>) -> List<R>) =
    PageResponse(mapper(content), number, size, totalElements, totalPages)
