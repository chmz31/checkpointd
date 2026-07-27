package com.chmz31.checkpointd.game.dto;

import com.chmz31.checkpointd.game.entity.Game;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GameResponse(
		UUID id,
		String externalProvider,
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
		List<GameWebsiteResponse> websites,
		Double externalRating,
		Integer externalRatingCount,
		List<String> screenshotUrls,
		List<String> artworkUrls,
		String backdropUrl) {

	public static GameResponse from(Game game) {
		return new GameResponse(
				game.getId(),
				game.getExternalProvider(),
				game.getExternalId(),
				game.getTitle(),
				game.getSlug(),
				game.getCoverUrl(),
				game.getReleaseDate(),
				game.getSummary(),
				List.copyOf(game.getGenres()),
				List.copyOf(game.getPlatforms()),
				List.copyOf(game.getDevelopers()),
				List.copyOf(game.getPublishers()),
				List.copyOf(game.getGameModes()),
				List.copyOf(game.getThemes()),
				List.copyOf(game.getPlayerPerspectives()),
				game.getWebsites().stream().map(GameWebsiteResponse::from).toList(),
				game.getExternalRating(),
				game.getExternalRatingCount(),
				List.copyOf(game.getScreenshotUrls()),
				List.copyOf(game.getArtworkUrls()),
				game.getBackdropUrl());
	}
}
