package com.chmz31.checkpointd.comment.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.comment.entity.ListComment;
import com.chmz31.checkpointd.comment.entity.ReviewComment;
import com.chmz31.checkpointd.comment.repository.ListCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
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
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
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
class AdminCommentControllerTests {

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
	void reportedListCommentsRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/admin/comments/lists/reported"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void reportedListCommentsRejectsNonAdmin() throws Exception {
		mockMvc.perform(get("/api/v1/admin/comments/lists/reported")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()).claim("role", "USER"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Admin access required"));
	}

	@Test
	void adminCanListReportedListComments() throws Exception {
		when(listCommentRepository.findReportedOrderByCreatedAtDesc(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(listComment())));
		when(listCommentReportRepository.countByCommentId(COMMENT_ID)).thenReturn(3L);

		mockMvc.perform(get("/api/v1/admin/comments/lists/reported")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()).claim("role", "ADMIN"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].reportCount").value(3))
				.andExpect(jsonPath("$.content[0].listName").value("Favorites"));
	}

	@Test
	void reportedReviewCommentsRejectsNonAdmin() throws Exception {
		mockMvc.perform(get("/api/v1/admin/comments/reviews/reported")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanListReportedReviewComments() throws Exception {
		when(reviewCommentRepository.findReportedOrderByCreatedAtDesc(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(reviewComment())));
		when(reviewCommentReportRepository.countByCommentId(COMMENT_ID)).thenReturn(1L);

		mockMvc.perform(get("/api/v1/admin/comments/reviews/reported")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()).claim("role", "ADMIN"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].gameTitle").value("Chrono Trigger"));
	}

	private User user(UUID id) {
		User user = new User(id + "@example.com", "player-" + id, "hash", Role.USER);
		user.setDisplayName("Player " + id);
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
