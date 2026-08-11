package com.chmz31.checkpointd.comment.dto;

import com.chmz31.checkpointd.comment.entity.ListComment;
import java.time.Instant;
import java.util.UUID;

public record ReportedListCommentResponse(
		UUID id,
		String username,
		String displayName,
		String body,
		Instant createdAt,
		long reportCount,
		UUID listId,
		String listName) {

	public static ReportedListCommentResponse from(ListComment comment, long reportCount) {
		return new ReportedListCommentResponse(
				comment.getId(),
				comment.getUser().getUsername(),
				comment.getUser().getDisplayName(),
				comment.getBody(),
				comment.getCreatedAt(),
				reportCount,
				comment.getList().getId(),
				comment.getList().getName());
	}
}
