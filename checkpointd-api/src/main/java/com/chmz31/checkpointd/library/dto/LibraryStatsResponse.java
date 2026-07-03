package com.chmz31.checkpointd.library.dto;

public record LibraryStatsResponse(
		long totalEntries,
		long wishlistCount,
		long backlogCount,
		long playingCount,
		long completedCount,
		long droppedCount,
		long pausedCount,
		long ratedCount,
		Double averageRating) {
}
