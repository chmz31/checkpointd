package com.chmz31.checkpointd.comment.dto;

import com.chmz31.checkpointd.comment.entity.ReviewComment;
import java.time.Instant;
import java.util.UUID;

public record ReportedReviewCommentResponse(
		UUID id,
		String username,
		String displayName,
		String body,
		Instant createdAt,
		long reportCount,
		UUID reviewId,
		UUID gameId,
		String gameTitle) {

	public static ReportedReviewCommentResponse from(ReviewComment comment, long reportCount) {
		return new ReportedReviewCommentResponse(
				comment.getId(),
				comment.getUser().getUsername(),
				comment.getUser().getDisplayName(),
				comment.getBody(),
				comment.getCreatedAt(),
				reportCount,
				comment.getReview().getId(),
				comment.getReview().getGame().getId(),
				comment.getReview().getGame().getTitle());
	}
}
