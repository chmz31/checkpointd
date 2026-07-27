package com.chmz31.checkpointd.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginatedResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last) {

	public static <T> PaginatedResponse<T> from(Page<?> page, List<T> content) {
		return new PaginatedResponse<>(
				content,
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}
}
