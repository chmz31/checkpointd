package com.chmz31.checkpointd.notification.controller;

import com.chmz31.checkpointd.common.dto.PaginatedResponse;
import com.chmz31.checkpointd.notification.dto.NotificationResponse;
import com.chmz31.checkpointd.notification.dto.UnreadCountResponse;
import com.chmz31.checkpointd.notification.service.NotificationService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	PaginatedResponse<NotificationResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var notifications = notificationService.getNotifications(currentUserId(jwt), page, size);
		return PaginatedResponse.from(notifications, notifications.getContent());
	}

	@GetMapping("/unread-count")
	UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
		return new UnreadCountResponse(notificationService.getUnreadCount(currentUserId(jwt)));
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
		notificationService.markAllAsRead(currentUserId(jwt));
	}

	private UUID currentUserId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
