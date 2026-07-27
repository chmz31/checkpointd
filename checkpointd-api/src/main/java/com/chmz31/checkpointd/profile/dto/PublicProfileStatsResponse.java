package com.chmz31.checkpointd.profile.dto;

public record PublicProfileStatsResponse(
		long totalGames,
		long completedGames,
		long ratedGames,
		Double averageRating) {
}
