package com.chmz31.checkpointd.externalgames.dto;

import java.time.LocalDate;
import java.util.List;

public record ExternalGameSearchResult(
		String provider,
		String externalId,
		String title,
		String slug,
		String coverUrl,
		LocalDate releaseDate,
		String summary,
		List<String> genres,
		List<String> platforms,
		List<String> screenshotUrls,
		List<String> artworkUrls,
		String backdropUrl) {

	public ExternalGameSearchResult {
		genres = genres == null ? List.of() : List.copyOf(genres);
		platforms = platforms == null ? List.of() : List.copyOf(platforms);
		screenshotUrls = screenshotUrls == null ? List.of() : List.copyOf(screenshotUrls);
		artworkUrls = artworkUrls == null ? List.of() : List.copyOf(artworkUrls);
	}

	public ExternalGameSearchResult(
			String provider,
			String externalId,
			String title,
			String slug,
			String coverUrl,
			LocalDate releaseDate,
			String summary,
			List<String> genres,
			List<String> platforms) {
		this(provider, externalId, title, slug, coverUrl, releaseDate, summary, genres, platforms,
				List.of(), List.of(), null);
	}

	public ExternalGameSearchResult(
			String provider,
			String externalId,
			String title,
			String slug,
			String coverUrl,
			LocalDate releaseDate) {
		this(provider, externalId, title, slug, coverUrl, releaseDate, null, List.of(), List.of(),
				List.of(), List.of(), null);
	}
}
