package com.chmz31.checkpointd.profile.dto;

import com.chmz31.checkpointd.review.dto.ReviewResponse;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import java.time.Instant;
import java.util.List;

public record PublicProfileResponse(
		String username,
		String displayName,
		String bio,
		ProfileVisibility profileVisibility,
		Instant joinedAt,
		PublicProfileStatsResponse stats,
		List<PublicProfileGameResponse> recentGames,
		List<ReviewResponse> recentReviews) {
}
