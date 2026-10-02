package hu.bme.aut.common.web.paging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class PageResponseTest {

    @Test
    fun aBatchMapperSeesTheWholePageAndThePagingStays() {
        val batches = mutableListOf<List<String>>()

        val response = PageImpl(listOf("a", "b"), PageRequest.of(1, 2), 5).toBatchResponse { items ->
            batches += items
            items.map { it.uppercase() }
        }

        assertThat(batches).containsExactly(listOf("a", "b"))
        assertThat(response).isEqualTo(PageResponse(listOf("A", "B"), page = 1, size = 2, totalItems = 5, totalPages = 3))
    }
}
