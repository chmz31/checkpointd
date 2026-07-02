package com.chmz31.checkpointd.library.dto;

import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import java.time.Instant;
import java.util.UUID;

public record LibraryEntryResponse(
		UUID id,
		UUID gameId,
		String gameTitle,
		String gameSlug,
		String gameCoverUrl,
		LibraryStatus status,
		Integer rating,
		String notes,
		Instant startedAt,
		Instant completedAt,
		Instant createdAt,
		Instant updatedAt) {

	public static LibraryEntryResponse from(LibraryEntry entry) {
		return new LibraryEntryResponse(
				entry.getId(),
				entry.getGame().getId(),
				entry.getGame().getTitle(),
				entry.getGame().getSlug(),
				entry.getGame().getCoverUrl(),
				entry.getStatus(),
				entry.getRating(),
				entry.getNotes(),
				entry.getStartedAt(),
				entry.getCompletedAt(),
				entry.getCreatedAt(),
				entry.getUpdatedAt());
	}
}
