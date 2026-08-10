package com.chmz31.checkpointd.library.model;

import org.springframework.data.domain.Sort;

public enum LibrarySortOption {
	UPDATED_DESC,
	ADDED_DESC,
	TITLE_ASC,
	TITLE_DESC,
	STATUS_ASC;

	public Sort toSort() {
		return switch (this) {
			case ADDED_DESC -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("game.title"));
			case TITLE_ASC -> Sort.by(Sort.Order.asc("game.title"));
			case TITLE_DESC -> Sort.by(Sort.Order.desc("game.title"));
			case STATUS_ASC -> Sort.by(Sort.Order.asc("status"), Sort.Order.asc("game.title"));
			case UPDATED_DESC -> Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("game.title"));
		};
	}
}
