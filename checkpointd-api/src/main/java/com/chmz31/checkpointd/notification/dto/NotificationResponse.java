package com.chmz31.checkpointd.notification.dto;

import com.chmz31.checkpointd.notification.entity.Notification;
import com.chmz31.checkpointd.notification.model.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		NotificationType type,
		String actorUsername,
		String actorDisplayName,
		UUID listId,
		String listName,
		UUID reviewId,
		UUID gameId,
		String gameSlug,
		String gameTitle,
		boolean read,
		Instant createdAt) {

	public static NotificationResponse from(Notification notification) {
		var list = notification.getList();
		var review = notification.getReview();

		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getActor().getUsername(),
				notification.getActor().getDisplayName(),
				list != null ? list.getId() : null,
				list != null ? list.getName() : null,
				review != null ? review.getId() : null,
				review != null ? review.getGame().getId() : null,
				review != null ? review.getGame().getSlug() : null,
				review != null ? review.getGame().getTitle() : null,
				notification.isRead(),
				notification.getCreatedAt());
	}
}
