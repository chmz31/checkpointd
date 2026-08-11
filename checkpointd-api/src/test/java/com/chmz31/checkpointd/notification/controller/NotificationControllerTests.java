package com.chmz31.checkpointd.notification.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.comment.repository.ListCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentRepository;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.notification.entity.Notification;
import com.chmz31.checkpointd.notification.model.NotificationType;
import com.chmz31.checkpointd.notification.repository.NotificationRepository;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationRepository notificationRepository;

	@MockitoBean
	private ListCommentRepository listCommentRepository;

	@MockitoBean
	private ReviewCommentRepository reviewCommentRepository;

	@MockitoBean
	private ListCommentReportRepository listCommentReportRepository;

	@MockitoBean
	private ReviewCommentReportRepository reviewCommentReportRepository;

	@MockitoBean
	private ListCommentLikeRepository listCommentLikeRepository;

	@MockitoBean
	private ReviewCommentLikeRepository reviewCommentLikeRepository;

	@MockitoBean
	private ListLikeRepository listLikeRepository;

	@MockitoBean
	private ReviewLikeRepository reviewLikeRepository;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private FollowRepository followRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private ReviewRepository reviewRepository;

	@MockitoBean
	private GameListRepository gameListRepository;

	@MockitoBean
	private GameListItemRepository gameListItemRepository;

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

	@Test
	void listRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/notifications"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanListNotifications() throws Exception {
		GameList list = list();
		Notification notification = new Notification(user(USER_ID), user(OTHER_USER_ID), NotificationType.LIST_LIKE, list, null);
		ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(notification, "createdAt", java.time.Instant.parse("2026-01-01T00:00:00Z"));

		when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(notification)));

		mockMvc.perform(get("/api/v1/notifications")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].type").value("LIST_LIKE"))
				.andExpect(jsonPath("$.content[0].actorUsername").value("player-" + OTHER_USER_ID));
	}

	@Test
	void unreadCountRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/notifications/unread-count"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanGetUnreadCount() throws Exception {
		when(notificationRepository.countByRecipientIdAndReadFalse(USER_ID)).thenReturn(3L);

		mockMvc.perform(get("/api/v1/notifications/unread-count")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(3));
	}

	@Test
	void markAllAsReadRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/notifications/read-all").with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanMarkAllAsRead() throws Exception {
		mockMvc.perform(post("/api/v1/notifications/read-all")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	private User user(UUID id) {
		User user = new User(id + "@example.com", "player-" + id, "hash", Role.USER);
		user.setProfileVisibility(ProfileVisibility.PUBLIC);
		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}

	private GameList list() {
		GameList list = new GameList(user(USER_ID), "Favorites");
		list.setVisibility(ListVisibility.PUBLIC);
		ReflectionTestUtils.setField(list, "id", LIST_ID);

		return list;
	}
}
