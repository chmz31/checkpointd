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
		List<String> developers,
		List<String> publishers,
		List<String> gameModes,
		List<String> themes,
		List<String> playerPerspectives,
		List<ExternalGameWebsite> websites,
		Double externalRating,
		Integer externalRatingCount,
		List<String> screenshotUrls,
		List<String> artworkUrls,
		String backdropUrl) {

	public ExternalGameSearchResult {
		genres = genres == null ? List.of() : List.copyOf(genres);
		platforms = platforms == null ? List.of() : List.copyOf(platforms);
		developers = developers == null ? List.of() : List.copyOf(developers);
		publishers = publishers == null ? List.of() : List.copyOf(publishers);
		gameModes = gameModes == null ? List.of() : List.copyOf(gameModes);
		themes = themes == null ? List.of() : List.copyOf(themes);
		playerPerspectives = playerPerspectives == null ? List.of() : List.copyOf(playerPerspectives);
		websites = websites == null ? List.of() : List.copyOf(websites);
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
				List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, null,
				List.of(), List.of(), null);
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
			List<String> platforms,
			List<String> screenshotUrls,
			List<String> artworkUrls,
			String backdropUrl) {
		this(provider, externalId, title, slug, coverUrl, releaseDate, summary, genres, platforms,
				List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, null,
				screenshotUrls, artworkUrls, backdropUrl);
	}

	public ExternalGameSearchResult(
			String provider,
			String externalId,
			String title,
			String slug,
			String coverUrl,
			LocalDate releaseDate) {
		this(provider, externalId, title, slug, coverUrl, releaseDate, null, List.of(), List.of(),
				List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null, null,
				List.of(), List.of(), null);
	}
}
