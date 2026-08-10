package com.chmz31.checkpointd.profile.dto;

import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicProfileGameResponse(
		UUID libraryEntryId,
		UUID gameId,
		String gameSlug,
		String gameTitle,
		String gameCoverUrl,
		LibraryStatus status,
		Instant updatedAt,
		List<String> genres,
		List<String> platforms) {

	public static PublicProfileGameResponse from(LibraryEntry entry) {
		return new PublicProfileGameResponse(
				entry.getId(),
				entry.getGame().getId(),
				entry.getGame().getSlug(),
				entry.getGame().getTitle(),
				entry.getGame().getCoverUrl(),
				entry.getStatus(),
				entry.getUpdatedAt(),
				List.copyOf(entry.getGame().getGenres()),
				List.copyOf(entry.getGame().getPlatforms()));
	}
}
