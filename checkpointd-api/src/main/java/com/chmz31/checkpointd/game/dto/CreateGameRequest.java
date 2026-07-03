package com.chmz31.checkpointd.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateGameRequest(
		@Size(max = 255) String externalProvider,
		@Size(max = 255) String externalId,
		@NotBlank @Size(max = 255) String title,
		@Size(max = 255) String slug,
		@Size(max = 2048) String coverUrl,
		LocalDate releaseDate,
		String summary,
		List<@Size(max = 255) String> genres,
		List<@Size(max = 255) String> platforms) {

	public CreateGameRequest(
			String externalProvider,
			String externalId,
			String title,
			String slug,
			String coverUrl,
			LocalDate releaseDate) {
		this(externalProvider, externalId, title, slug, coverUrl, releaseDate, null, List.of(), List.of());
	}
}
