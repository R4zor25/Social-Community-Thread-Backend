package hu.bme.aut.common.web.paging

import org.springframework.data.domain.Page

data class PageResponse<T>(val items: List<T>, val page: Int, val size: Int, val totalItems: Long, val totalPages: Int)

fun <T : Any, R> Page<T>.toResponse(mapper: (T) -> R) =
    PageResponse(content.map(mapper), number, size, totalElements, totalPages)
