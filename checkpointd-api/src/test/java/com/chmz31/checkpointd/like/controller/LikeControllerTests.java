package com.chmz31.checkpointd.like.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.entity.ListLike;
import com.chmz31.checkpointd.like.entity.ReviewLike;
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
class LikeControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Autowired
	private MockMvc mockMvc;

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
	void likeListRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/likes/lists/{listId}", LIST_ID).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanLikeList() throws Exception {
		GameList list = list(ListVisibility.PUBLIC);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.empty());
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list));
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(false, true);
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(1L);

		mockMvc.perform(post("/api/v1/likes/lists/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(true))
				.andExpect(jsonPath("$.likeCount").value(1));
	}

	@Test
	void likingListTwiceReturnsConflict() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(true);

		mockMvc.perform(post("/api/v1/likes/lists/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("You already liked this list"));
	}

	@Test
	void unlikeListRequiresAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/likes/lists/{listId}", LIST_ID).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanUnlikeList() throws Exception {
		GameList list = list(ListVisibility.PUBLIC);
		ListLike like = new ListLike(user(USER_ID), list);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(listLikeRepository.findByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(Optional.of(like));

		mockMvc.perform(delete("/api/v1/likes/lists/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false));
	}

	@Test
	void listStatusWorksWithoutAuthentication() throws Exception {
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(5L);

		mockMvc.perform(get("/api/v1/likes/lists/{listId}/status", LIST_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.likeCount").value(5));
	}

	@Test
	void likeReviewRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/likes/reviews/{reviewId}", REVIEW_ID).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanLikeReview() throws Exception {
		Review review = review(ReviewVisibility.PUBLIC);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.empty());
		when(reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				REVIEW_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(review));
		when(reviewLikeRepository.existsByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(false, true);
		when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(1L);

		mockMvc.perform(post("/api/v1/likes/reviews/{reviewId}", REVIEW_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(true))
				.andExpect(jsonPath("$.likeCount").value(1));
	}

	@Test
	void unlikeReviewRequiresAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/likes/reviews/{reviewId}", REVIEW_ID).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanUnlikeReview() throws Exception {
		Review review = review(ReviewVisibility.PUBLIC);
		ReviewLike like = new ReviewLike(user(USER_ID), review);

		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.of(review));
		when(reviewLikeRepository.findByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(Optional.of(like));

		mockMvc.perform(delete("/api/v1/likes/reviews/{reviewId}", REVIEW_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false));
	}

	@Test
	void reviewStatusWorksWithoutAuthentication() throws Exception {
		when(reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				REVIEW_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(review(ReviewVisibility.PUBLIC)));
		when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(7L);

		mockMvc.perform(get("/api/v1/likes/reviews/{reviewId}/status", REVIEW_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.likeCount").value(7));
	}

	private User user(UUID id) {
		User user = new User(id + "@example.com", "player-" + id, "hash", Role.USER);
		user.setProfileVisibility(ProfileVisibility.PUBLIC);
		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}

	private GameList list(ListVisibility visibility) {
		GameList list = new GameList(user(OTHER_USER_ID), "Favorites");
		list.setVisibility(visibility);
		ReflectionTestUtils.setField(list, "id", LIST_ID);

		return list;
	}

	private Review review(ReviewVisibility visibility) {
		Game game = new Game("Chrono Trigger");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		Review review = new Review(user(OTHER_USER_ID), game, "A public opinion.");
		review.setVisibility(visibility);
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);

		return review;
	}
}
