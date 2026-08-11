package com.chmz31.checkpointd.notification.service;

import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.notification.dto.NotificationResponse;
import com.chmz31.checkpointd.notification.entity.Notification;
import com.chmz31.checkpointd.notification.model.NotificationType;
import com.chmz31.checkpointd.notification.repository.NotificationRepository;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.user.entity.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 50;

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public void notifyFollow(User actor, User recipient) {
		notifyIfNotSelf(actor, recipient, NotificationType.FOLLOW, null, null);
	}

	@Transactional
	public void notifyListLiked(User actor, GameList list) {
		notifyIfNotSelf(actor, list.getUser(), NotificationType.LIST_LIKE, list, null);
	}

	@Transactional
	public void notifyReviewLiked(User actor, Review review) {
		notifyIfNotSelf(actor, review.getUser(), NotificationType.REVIEW_LIKE, null, review);
	}

	@Transactional
	public void notifyListCommented(User actor, GameList list) {
		notifyIfNotSelf(actor, list.getUser(), NotificationType.LIST_COMMENT, list, null);
	}

	@Transactional
	public void notifyReviewCommented(User actor, Review review) {
		notifyIfNotSelf(actor, review.getUser(), NotificationType.REVIEW_COMMENT, null, review);
	}

	@Transactional
	public void notifyListCommentReplied(User actor, User parentAuthor, GameList list) {
		notifyIfNotSelf(actor, parentAuthor, NotificationType.COMMENT_REPLY, list, null);
	}

	@Transactional
	public void notifyReviewCommentReplied(User actor, User parentAuthor, Review review) {
		notifyIfNotSelf(actor, parentAuthor, NotificationType.COMMENT_REPLY, null, review);
	}

	@Transactional
	public void notifyListCommentLiked(User actor, User commentAuthor, GameList list) {
		notifyIfNotSelf(actor, commentAuthor, NotificationType.LIST_COMMENT_LIKE, list, null);
	}

	@Transactional
	public void notifyReviewCommentLiked(User actor, User commentAuthor, Review review) {
		notifyIfNotSelf(actor, commentAuthor, NotificationType.REVIEW_COMMENT_LIKE, null, review);
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> getNotifications(UUID userId, int page, int size) {
		return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageRequest(page, size))
				.map(NotificationResponse::from);
	}

	@Transactional(readOnly = true)
	public long getUnreadCount(UUID userId) {
		return notificationRepository.countByRecipientIdAndReadFalse(userId);
	}

	@Transactional
	public void markAllAsRead(UUID userId) {
		notificationRepository.markAllAsRead(userId);
	}

	private void notifyIfNotSelf(User actor, User recipient, NotificationType type, GameList list, Review review) {
		if (actor.getId().equals(recipient.getId())) {
			return;
		}
		notificationRepository.save(new Notification(recipient, actor, type, list, review));
	}

	private PageRequest pageRequest(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize);
	}
}
