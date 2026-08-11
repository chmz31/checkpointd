package com.chmz31.checkpointd.comment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.comment.entity.ListComment;
import com.chmz31.checkpointd.comment.entity.ListCommentLike;
import com.chmz31.checkpointd.comment.entity.ReviewComment;
import com.chmz31.checkpointd.comment.repository.ListCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentRepository;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.notification.repository.NotificationRepository;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentLikeControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID COMMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Autowired
	private MockMvc mockMvc;

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
	private NotificationRepository notificationRepository;

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
	void likeListCommentRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/likes/list-comments/{commentId}", COMMENT_ID).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanLikeListComment() throws Exception {
		ListComment comment = listComment();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
		when(listCommentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(false, true);
		when(listCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(1L);

		mockMvc.perform(post("/api/v1/likes/list-comments/{commentId}", COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(true))
				.andExpect(jsonPath("$.likeCount").value(1));
	}

	@Test
	void authenticatedUserCanUnlikeListComment() throws Exception {
		ListComment comment = listComment();
		ListCommentLike like = new ListCommentLike(user(USER_ID), comment);

		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
		when(listCommentLikeRepository.findByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(Optional.of(like));

		mockMvc.perform(delete("/api/v1/likes/list-comments/{commentId}", COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false));
	}

	@Test
	void listCommentLikeStatusWorksWithoutAuthentication() throws Exception {
		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(listComment()));
		when(listCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(5L);

		mockMvc.perform(get("/api/v1/likes/list-comments/{commentId}/status", COMMENT_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.likeCount").value(5));
	}

	@Test
	void likeReviewCommentRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/likes/review-comments/{commentId}", COMMENT_ID).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanLikeReviewComment() throws Exception {
		ReviewComment comment = reviewComment();
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(reviewCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
		when(reviewCommentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(false, true);
		when(reviewCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(1L);

		mockMvc.perform(post("/api/v1/likes/review-comments/{commentId}", COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(true));
	}

	@Test
	void reviewCommentLikeStatusWorksWithoutAuthentication() throws Exception {
		when(reviewCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(reviewComment()));
		when(reviewCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(2L);

		mockMvc.perform(get("/api/v1/likes/review-comments/{commentId}/status", COMMENT_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.likeCount").value(2));
	}

	private User user(UUID id) {
		User user = new User(id + "@example.com", "player-" + id, "hash", Role.USER);
		user.setProfileVisibility(ProfileVisibility.PUBLIC);
		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}

	private GameList list() {
		GameList list = new GameList(user(OTHER_USER_ID), "Favorites");
		list.setVisibility(ListVisibility.PUBLIC);
		ReflectionTestUtils.setField(list, "id", LIST_ID);

		return list;
	}

	private Game game() {
		Game game = new Game("Chrono Trigger");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}

	private Review review() {
		Review review = new Review(user(OTHER_USER_ID), game(), "A public opinion.");
		review.setVisibility(ReviewVisibility.PUBLIC);
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);

		return review;
	}

	private ListComment listComment() {
		ListComment comment = new ListComment(user(OTHER_USER_ID), list(), "Great list!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}

	private ReviewComment reviewComment() {
		ReviewComment comment = new ReviewComment(user(OTHER_USER_ID), review(), "Great review!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}
}
