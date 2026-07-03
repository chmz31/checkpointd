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
		List<String> platforms) {

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
				List.copyOf(game.getPlatforms()));
	}
}
