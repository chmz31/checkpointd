package com.chmz31.checkpointd.externalgames.dto;

import java.time.LocalDate;

public record ExternalGameSearchResult(
		String provider,
		String externalId,
		String title,
		String slug,
		String coverUrl,
		LocalDate releaseDate) {
}
