package com.chmz31.checkpointd.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateGameRequest(
		@Size(max = 255) String externalProvider,
		@Size(max = 255) String externalId,
		@NotBlank @Size(max = 255) String title,
		@Size(max = 255) String slug,
		@Size(max = 2048) String coverUrl,
		LocalDate releaseDate) {
}
