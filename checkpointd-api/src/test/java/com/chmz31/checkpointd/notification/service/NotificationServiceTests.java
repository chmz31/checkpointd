package com.chmz31.checkpointd.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.notification.dto.NotificationResponse;
import com.chmz31.checkpointd.notification.entity.Notification;
import com.chmz31.checkpointd.notification.model.NotificationType;
import com.chmz31.checkpointd.notification.repository.NotificationRepository;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationService notificationService;

	@Test
	void notifyFollowCreatesNotificationForDifferentUsers() {
		User actor = user(USER_ID);
		User recipient = user(OTHER_USER_ID);

		notificationService.notifyFollow(actor, recipient);

		Notification saved = captureSaved();
		assertThat(saved.getRecipient()).isSameAs(recipient);
		assertThat(saved.getActor()).isSameAs(actor);
		assertThat(saved.getType()).isEqualTo(NotificationType.FOLLOW);
		assertThat(saved.getList()).isNull();
		assertThat(saved.getReview()).isNull();
	}

	@Test
	void notifyFollowSkipsWhenActorIsRecipient() {
		User user = user(USER_ID);

		notificationService.notifyFollow(user, user);

		verify(notificationRepository, never()).save(any(Notification.class));
	}

	@Test
	void notifyListLikedCreatesNotificationForListOwner() {
		GameList list = list(OTHER_USER_ID);
		User actor = user(USER_ID);

		notificationService.notifyListLiked(actor, list);

		Notification saved = captureSaved();
		assertThat(saved.getRecipient()).isSameAs(list.getUser());
		assertThat(saved.getType()).isEqualTo(NotificationType.LIST_LIKE);
		assertThat(saved.getList()).isSameAs(list);
	}

	@Test
	void notifyListLikedSkipsWhenActorOwnsList() {
		GameList list = list(USER_ID);

		notificationService.notifyListLiked(list.getUser(), list);

		verify(notificationRepository, never()).save(any(Notification.class));
	}

	@Test
	void notifyReviewLikedCreatesNotificationForReviewOwner() {
		Review review = review(OTHER_USER_ID);
		User actor = user(USER_ID);

		notificationService.notifyReviewLiked(actor, review);

		Notification saved = captureSaved();
		assertThat(saved.getRecipient()).isSameAs(review.getUser());
		assertThat(saved.getType()).isEqualTo(NotificationType.REVIEW_LIKE);
		assertThat(saved.getReview()).isSameAs(review);
	}

	@Test
	void notifyListCommentedCreatesNotificationForListOwner() {
		GameList list = list(OTHER_USER_ID);
		User actor = user(USER_ID);

		notificationService.notifyListCommented(actor, list);

		Notification saved = captureSaved();
		assertThat(saved.getType()).isEqualTo(NotificationType.LIST_COMMENT);
		assertThat(saved.getList()).isSameAs(list);
	}

	@Test
	void notifyReviewCommentedCreatesNotificationForReviewOwner() {
		Review review = review(OTHER_USER_ID);
		User actor = user(USER_ID);

		notificationService.notifyReviewCommented(actor, review);

		Notification saved = captureSaved();
		assertThat(saved.getType()).isEqualTo(NotificationType.REVIEW_COMMENT);
		assertThat(saved.getReview()).isSameAs(review);
	}

	@Test
	void notifyListCommentRepliedCreatesNotificationForParentAuthor() {
		GameList list = list(OTHER_USER_ID);
		User actor = user(USER_ID);
		User parentAuthor = user(UUID.fromString("00000000-0000-0000-0000-000000000003"));

		notificationService.notifyListCommentReplied(actor, parentAuthor, list);

		Notification saved = captureSaved();
		assertThat(saved.getRecipient()).isSameAs(parentAuthor);
		assertThat(saved.getType()).isEqualTo(NotificationType.COMMENT_REPLY);
		assertThat(saved.getList()).isSameAs(list);
	}

	@Test
	void notifyReviewCommentRepliedCreatesNotificationForParentAuthor() {
		Review review = review(OTHER_USER_ID);
		User actor = user(USER_ID);
		User parentAuthor = user(UUID.fromString("00000000-0000-0000-0000-000000000003"));

		notificationService.notifyReviewCommentReplied(actor, parentAuthor, review);

		Notification saved = captureSaved();
		assertThat(saved.getRecipient()).isSameAs(parentAuthor);
		assertThat(saved.getType()).isEqualTo(NotificationType.COMMENT_REPLY);
		assertThat(saved.getReview()).isSameAs(review);
	}

	@Test
	void notifyListCommentLikedCreatesNotificationForCommentAuthor() {
		GameList list = list(OTHER_USER_ID);
		User actor = user(USER_ID);
		User commentAuthor = user(UUID.fromString("00000000-0000-0000-0000-000000000003"));

		notificationService.notifyListCommentLiked(actor, commentAuthor, list);

		Notification saved = captureSaved();
		assertThat(saved.getRecipient()).isSameAs(commentAuthor);
		assertThat(saved.getType()).isEqualTo(NotificationType.LIST_COMMENT_LIKE);
	}

	@Test
	void notifyReviewCommentLikedSkipsWhenActorIsCommentAuthor() {
		Review review = review(OTHER_USER_ID);
		User actor = user(USER_ID);

		notificationService.notifyReviewCommentLiked(actor, actor, review);

		verify(notificationRepository, never()).save(any(Notification.class));
	}

	@Test
	void getUnreadCountDelegatesToRepository() {
		when(notificationRepository.countByRecipientIdAndReadFalse(USER_ID)).thenReturn(4L);

		assertThat(notificationService.getUnreadCount(USER_ID)).isEqualTo(4L);
	}

	@Test
	void markAllAsReadDelegatesToRepository() {
		notificationService.markAllAsRead(USER_ID);

		verify(notificationRepository).markAllAsRead(USER_ID);
	}

	@Test
	void getNotificationsReturnsMappedPage() {
		GameList list = list(USER_ID);
		Notification notification = new Notification(list.getUser(), user(OTHER_USER_ID), NotificationType.LIST_LIKE, list, null);
		ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(notification, "createdAt", java.time.Instant.parse("2026-01-01T00:00:00Z"));

		when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(notification)));

		Page<NotificationResponse> page = notificationService.getNotifications(USER_ID, 0, 20);

		assertThat(page.getContent()).extracting(NotificationResponse::type).containsExactly(NotificationType.LIST_LIKE);
	}

	private Notification captureSaved() {
		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(captor.capture());
		return captor.getValue();
	}

	private User user(UUID id) {
		User user = new User(id + "@example.com", "player-" + id, "hash", Role.USER);
		user.setProfileVisibility(ProfileVisibility.PUBLIC);
		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}

	private GameList list(UUID ownerId) {
		GameList list = new GameList(user(ownerId), "Favorites");
		list.setVisibility(ListVisibility.PUBLIC);
		ReflectionTestUtils.setField(list, "id", LIST_ID);

		return list;
	}

	private Review review(UUID ownerId) {
		Game game = new Game("Chrono Trigger");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		Review review = new Review(user(ownerId), game, "A public opinion.");
		review.setVisibility(ReviewVisibility.PUBLIC);
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);

		return review;
	}
}
