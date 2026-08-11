package com.chmz31.checkpointd.comment.dto;

import com.chmz31.checkpointd.comment.entity.ListComment;
import com.chmz31.checkpointd.comment.entity.ReviewComment;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
		UUID id,
		String username,
		String displayName,
		String body,
		Instant createdAt,
		boolean owner) {

	public static CommentResponse from(ListComment comment, boolean owner) {
		return new CommentResponse(
				comment.getId(),
				comment.getUser().getUsername(),
				comment.getUser().getDisplayName(),
				comment.getBody(),
				comment.getCreatedAt(),
				owner);
	}

	public static CommentResponse from(ReviewComment comment, boolean owner) {
		return new CommentResponse(
				comment.getId(),
				comment.getUser().getUsername(),
				comment.getUser().getDisplayName(),
				comment.getBody(),
				comment.getCreatedAt(),
				owner);
	}
}
