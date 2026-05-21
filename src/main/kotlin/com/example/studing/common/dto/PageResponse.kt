package com.example.studing.common.dto

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
) {
    companion object {
        fun <S : Any, T> from(page: Page<S>, mapper: (S) -> T): PageResponse<T> = PageResponse(
            items = page.content.map(mapper),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious(),
        )
    }
}
