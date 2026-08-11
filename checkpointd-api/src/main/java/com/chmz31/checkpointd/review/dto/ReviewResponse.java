package com.chmz31.checkpointd.review.dto;

import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
		UUID id,
		String username,
		String displayName,
		UUID gameId,
		String gameSlug,
		String gameTitle,
		String gameCoverUrl,
		Integer rating,
		String body,
		boolean containsSpoilers,
		ReviewVisibility visibility,
		long likeCount,
		boolean liked,
		Instant createdAt,
		Instant updatedAt,
		boolean owner) {

	public static ReviewResponse from(Review review, long likeCount, boolean liked, boolean owner) {
		return new ReviewResponse(
				review.getId(),
				review.getUser().getUsername(),
				review.getUser().getDisplayName(),
				review.getGame().getId(),
				review.getGame().getSlug(),
				review.getGame().getTitle(),
				review.getGame().getCoverUrl(),
				review.getRating(),
				review.getBody(),
				review.isContainsSpoilers(),
				review.getVisibility(),
				likeCount,
				liked,
				review.getCreatedAt(),
				review.getUpdatedAt(),
				owner);
	}
}
