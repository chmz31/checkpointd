package com.chmz31.checkpointd.review.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.comment.repository.ListCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.notification.repository.NotificationRepository;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewRepository reviewRepository;

	@MockitoBean
	private GameListRepository gameListRepository;

	@MockitoBean
	private GameListItemRepository gameListItemRepository;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private FollowRepository followRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

	@MockitoBean
	private ListLikeRepository listLikeRepository;

	@MockitoBean
	private ReviewLikeRepository reviewLikeRepository;

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

	@Test
	void authenticatedUserCanCreateReview() throws Exception {
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
				.thenReturn(Optional.of(eligibleEntry()));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());
		when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> withReviewMetadata(invocation.getArgument(0)));

		mockMvc.perform(put("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "rating": 9,
								  "body": "Sharp, generous, and very replayable.",
								  "containsSpoilers": true,
								  "visibility": "PUBLIC"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(REVIEW_ID.toString()))
				.andExpect(jsonPath("$.username").value("playerone"))
				.andExpect(jsonPath("$.gameId").value(GAME_ID.toString()))
				.andExpect(jsonPath("$.rating").value(9))
				.andExpect(jsonPath("$.body").value("Sharp, generous, and very replayable."))
				.andExpect(jsonPath("$.containsSpoilers").value(true))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"))
				.andExpect(jsonPath("$.owner").value(true))
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.notes").doesNotExist());
	}

	@Test
	void authenticatedUserCanUpdateOwnReviewThroughUpsert() throws Exception {
		Review review = review(ReviewVisibility.PRIVATE, ProfileVisibility.PUBLIC);
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
				.thenReturn(Optional.of(eligibleEntry()));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(review));
		when(reviewRepository.save(review)).thenReturn(review);

		mockMvc.perform(put("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "rating": 7,
								  "body": "Updated opinion.",
								  "containsSpoilers": false,
								  "visibility": "PUBLIC"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rating").value(7))
				.andExpect(jsonPath("$.body").value("Updated opinion."))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));
	}

	@Test
	void authenticatedUserCanDeleteOwnReview() throws Exception {
		Review review = review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC);
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(review));

		mockMvc.perform(delete("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isNoContent());

		verify(reviewRepository).delete(review);
	}

	@Test
	void unauthenticatedUserCannotCreateUpdateOrDelete() throws Exception {
		mockMvc.perform(put("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "body": "Nope"
								}
								"""))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(delete("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publicGameReviewsOnlyIncludePublicReviewsFromPublicProfiles() throws Exception {
		Review review = review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC);
		when(reviewRepository.findByGameIdAndVisibilityAndUserProfileVisibilityOrderByUpdatedAtDesc(
				eq(GAME_ID), eq(ReviewVisibility.PUBLIC), eq(ProfileVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));
		when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(2L);

		mockMvc.perform(get("/api/v1/reviews/games/{gameId}", GAME_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].visibility").value("PUBLIC"))
				.andExpect(jsonPath("$.content[0].username").value("playerone"))
				.andExpect(jsonPath("$.content[0].likeCount").value(2))
				.andExpect(jsonPath("$.content[0].liked").value(false));
	}

	@Test
	void publicUserReviewsReturnNotFoundForPrivateProfile() throws Exception {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PRIVATE)));

		mockMvc.perform(get("/api/v1/reviews/users/playerone"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Profile not found"));
	}

	@Test
	void publicSingleUserGameReviewWorksForPublicReviewAndProfile() throws Exception {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(reviewRepository.findByUserUsernameAndGameIdAndVisibilityAndUserProfileVisibility(
				"playerone", GAME_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.of(review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)));

		mockMvc.perform(get("/api/v1/reviews/users/playerone/games/{gameId}", GAME_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("playerone"))
				.andExpect(jsonPath("$.gameTitle").value("Chrono Trigger"));
	}

	@Test
	void publicSingleUserGameReviewReturnsNotFoundForPrivateReview() throws Exception {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(reviewRepository.findByUserUsernameAndGameIdAndVisibilityAndUserProfileVisibility(
				"playerone", GAME_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/reviews/users/playerone/games/{gameId}", GAME_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Review not found"));
	}

	@Test
	void myReviewsReturnsCurrentUsersReviewsRegardlessOfVisibility() throws Exception {
		Review review = review(ReviewVisibility.PRIVATE, ProfileVisibility.PUBLIC);
		when(reviewRepository.findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));

		mockMvc.perform(get("/api/v1/reviews/me")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.content[0].owner").value(true));
	}

	@Test
	void myReviewsRejectsUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/v1/reviews/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void myGameReviewReturnsPrivateOwnerReview() throws Exception {
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
				.thenReturn(Optional.of(review(ReviewVisibility.PRIVATE, ProfileVisibility.PUBLIC)));

		mockMvc.perform(get("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.owner").value(true));
	}

	@Test
	void bodyValidationWorks() throws Exception {
		mockMvc.perform(put("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "body": "   "
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void ratingValidationWorks() throws Exception {
		mockMvc.perform(put("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "rating": 11,
								  "body": "Too much."
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void wishlistedGameCannotBeReviewed() throws Exception {
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
				.thenReturn(Optional.of(new LibraryEntry(user(ProfileVisibility.PUBLIC), game(), LibraryStatus.WISHLIST)));

		mockMvc.perform(put("/api/v1/reviews/me/games/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "body": "Looks great."
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("You can only review games you have started playing"));

		verify(reviewRepository, never()).save(any(Review.class));
	}

	private LibraryEntry eligibleEntry() {
		return new LibraryEntry(user(ProfileVisibility.PUBLIC), game(), LibraryStatus.PLAYING);
	}

	private User user(ProfileVisibility profileVisibility) {
		User user = new User("player@example.com", "playerone", "hash", Role.USER);
		user.setDisplayName("Player One");
		user.setProfileVisibility(profileVisibility);
		ReflectionTestUtils.setField(user, "id", USER_ID);

		return user;
	}

	private Game game() {
		Game game = new Game("Chrono Trigger");
		game.setSlug("chrono-trigger");
		game.setCoverUrl("https://img.example/cover.jpg");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}

	private Review review(ReviewVisibility visibility, ProfileVisibility profileVisibility) {
		Review review = new Review(user(profileVisibility), game(), "A public opinion.");
		review.setRating(8);
		review.setVisibility(visibility);
		return withReviewMetadata(review);
	}

	private Review withReviewMetadata(Review review) {
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);
		ReflectionTestUtils.setField(review, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(review, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));

		return review;
	}
}
