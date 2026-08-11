package com.chmz31.checkpointd.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.review.dto.ReviewRequest;
import com.chmz31.checkpointd.review.dto.ReviewResponse;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
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
class ReviewServiceTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private GameRepository gameRepository;

	@Mock
	private LibraryEntryRepository libraryEntryRepository;

	@Mock
	private ReviewLikeRepository reviewLikeRepository;

	@InjectMocks
	private ReviewService reviewService;

	@Test
	void getPublicGameReviewsReturnsMappedPublicReviews() {
		when(reviewRepository.findByGameIdAndVisibilityAndUserProfileVisibilityOrderByUpdatedAtDesc(
				eq(GAME_ID), eq(ReviewVisibility.PUBLIC), eq(ProfileVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))));

		Page<ReviewResponse> reviews = reviewService.getPublicGameReviews(GAME_ID, null, 0, 10);

		assertThat(reviews.getContent()).extracting(ReviewResponse::owner).containsExactly(false);
	}

	@Test
	void getPublicGameReviewsClampsNegativePageAndLargeSize() {
		when(reviewRepository.findByGameIdAndVisibilityAndUserProfileVisibilityOrderByUpdatedAtDesc(
				eq(GAME_ID), eq(ReviewVisibility.PUBLIC), eq(ProfileVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		reviewService.getPublicGameReviews(GAME_ID, null, -2, 500);

		verify(reviewRepository).findByGameIdAndVisibilityAndUserProfileVisibilityOrderByUpdatedAtDesc(
				eq(GAME_ID), eq(ReviewVisibility.PUBLIC), eq(ProfileVisibility.PUBLIC), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
	}

	@Test
	void getPublicUserReviewsReturnsReviewsForPublicProfile() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(reviewRepository.findByUserUsernameAndUserProfileVisibilityAndVisibilityOrderByUpdatedAtDesc(
				eq("playerone"), eq(ProfileVisibility.PUBLIC), eq(ReviewVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))));

		Page<ReviewResponse> reviews = reviewService.getPublicUserReviews("playerone", null, 0, 10);

		assertThat(reviews.getContent()).extracting(ReviewResponse::username).containsExactly("playerone");
	}

	@Test
	void getPublicUserReviewsRejectsPrivateProfile() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PRIVATE)));

		assertThatThrownBy(() -> reviewService.getPublicUserReviews("playerone", null, 0, 10))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Profile not found");
	}

	@Test
	void getPublicUserReviewsRejectsMissingUser() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.getPublicUserReviews("playerone", null, 0, 10))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Profile not found");
	}

	@Test
	void getPublicUserGameReviewReturnsReviewForPublicProfile() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(reviewRepository.findByUserUsernameAndGameIdAndVisibilityAndUserProfileVisibility(
				"playerone", GAME_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.of(review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)));

		ReviewResponse response = reviewService.getPublicUserGameReview("playerone", GAME_ID, null);

		assertThat(response.owner()).isFalse();
		assertThat(response.gameId()).isEqualTo(GAME_ID);
	}

	@Test
	void getPublicUserGameReviewRejectsPrivateProfileBeforeQueryingReview() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PRIVATE)));

		assertThatThrownBy(() -> reviewService.getPublicUserGameReview("playerone", GAME_ID, null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Profile not found");

		verify(reviewRepository, never()).findByUserUsernameAndGameIdAndVisibilityAndUserProfileVisibility(
				any(String.class), any(UUID.class), any(ReviewVisibility.class), any(ProfileVisibility.class));
	}

	@Test
	void getPublicUserGameReviewRejectsMissingReview() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(reviewRepository.findByUserUsernameAndGameIdAndVisibilityAndUserProfileVisibility(
				"playerone", GAME_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.getPublicUserGameReview("playerone", GAME_ID, null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Review not found");
	}

	@Test
	void getMyReviewsReturnsOwnersReviews() {
		when(reviewRepository.findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review(ReviewVisibility.PRIVATE, ProfileVisibility.PUBLIC))));
		when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(6L);
		when(reviewLikeRepository.existsByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(true);

		Page<ReviewResponse> reviews = reviewService.getMyReviews(USER_ID, 0, 10);

		assertThat(reviews.getContent()).extracting(ReviewResponse::owner).containsExactly(true);
		assertThat(reviews.getContent()).extracting(ReviewResponse::likeCount).containsExactly(6L);
		assertThat(reviews.getContent()).extracting(ReviewResponse::liked).containsExactly(true);
	}

	@Test
	void getMyReviewsClampsNegativePageAndLargeSize() {
		when(reviewRepository.findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		reviewService.getMyReviews(USER_ID, -2, 500);

		verify(reviewRepository).findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
	}

	@Test
	void getMyReviewsUsesDefaultSizeWhenInvalid() {
		when(reviewRepository.findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC))));
		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

		reviewService.getMyReviews(USER_ID, 0, 0);

		verify(reviewRepository).findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
	}

	@Test
	void getMyGameReviewReturnsOwnerReviewRegardlessOfVisibility() {
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
				.thenReturn(Optional.of(review(ReviewVisibility.PRIVATE, ProfileVisibility.PUBLIC)));

		ReviewResponse response = reviewService.getMyGameReview(USER_ID, GAME_ID);

		assertThat(response.owner()).isTrue();
		assertThat(response.visibility()).isEqualTo(ReviewVisibility.PRIVATE);
	}

	@Test
	void getMyGameReviewRejectsMissingReview() {
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.getMyGameReview(USER_ID, GAME_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Review not found");
	}

	@Test
	void saveMyGameReviewCreatesNewReviewWhenNoneExists() {
		User user = user(ProfileVisibility.PUBLIC);
		Game game = game();
		ReviewRequest request = new ReviewRequest(9, "  Sharp and replayable.  ", true, ReviewVisibility.PUBLIC);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(eligibleEntry()));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());
		when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ReviewResponse response = reviewService.saveMyGameReview(USER_ID, GAME_ID, request);

		ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
		verify(reviewRepository).save(reviewCaptor.capture());
		Review saved = reviewCaptor.getValue();

		assertThat(saved.getUser()).isSameAs(user);
		assertThat(saved.getGame()).isSameAs(game);
		assertThat(saved.getBody()).isEqualTo("Sharp and replayable.");
		assertThat(saved.getRating()).isEqualTo(9);
		assertThat(saved.isContainsSpoilers()).isTrue();
		assertThat(saved.getVisibility()).isEqualTo(ReviewVisibility.PUBLIC);
		assertThat(response.owner()).isTrue();
	}

	@Test
	void saveMyGameReviewUpdatesExistingReviewInPlace() {
		Review existing = review(ReviewVisibility.PRIVATE, ProfileVisibility.PUBLIC);
		ReviewRequest request = new ReviewRequest(4, "Updated opinion.", false, ReviewVisibility.PUBLIC);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(eligibleEntry()));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(existing));
		when(reviewRepository.save(existing)).thenReturn(existing);

		reviewService.saveMyGameReview(USER_ID, GAME_ID, request);

		ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
		verify(reviewRepository).save(reviewCaptor.capture());

		assertThat(reviewCaptor.getValue()).isSameAs(existing);
		assertThat(existing.getBody()).isEqualTo("Updated opinion.");
		assertThat(existing.getRating()).isEqualTo(4);
		assertThat(existing.isContainsSpoilers()).isFalse();
		assertThat(existing.getVisibility()).isEqualTo(ReviewVisibility.PUBLIC);
	}

	@Test
	void saveMyGameReviewDefaultsVisibilityToPublicWhenNull() {
		ReviewRequest request = new ReviewRequest(null, "No visibility specified.", null, null);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(eligibleEntry()));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());
		when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ReviewResponse response = reviewService.saveMyGameReview(USER_ID, GAME_ID, request);

		assertThat(response.visibility()).isEqualTo(ReviewVisibility.PUBLIC);
		assertThat(response.containsSpoilers()).isFalse();
	}

	@Test
	void saveMyGameReviewRejectsMissingUser() {
		ReviewRequest request = new ReviewRequest(null, "Body", null, null);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(eligibleEntry()));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.saveMyGameReview(USER_ID, GAME_ID, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User not found");

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void saveMyGameReviewRejectsMissingGame() {
		ReviewRequest request = new ReviewRequest(null, "Body", null, null);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(eligibleEntry()));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.saveMyGameReview(USER_ID, GAME_ID, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Game not found");

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void saveMyGameReviewRejectsGameNotInLibrary() {
		ReviewRequest request = new ReviewRequest(null, "Body", null, null);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.saveMyGameReview(USER_ID, GAME_ID, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Add this game to your library before reviewing it");

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void saveMyGameReviewRejectsWishlistStatus() {
		ReviewRequest request = new ReviewRequest(null, "Body", null, null);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
				.thenReturn(Optional.of(libraryEntry(LibraryStatus.WISHLIST)));

		assertThatThrownBy(() -> reviewService.saveMyGameReview(USER_ID, GAME_ID, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("You can only review games you have started playing");

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void saveMyGameReviewRejectsBacklogStatus() {
		ReviewRequest request = new ReviewRequest(null, "Body", null, null);

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
				.thenReturn(Optional.of(libraryEntry(LibraryStatus.BACKLOG)));

		assertThatThrownBy(() -> reviewService.saveMyGameReview(USER_ID, GAME_ID, request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("You can only review games you have started playing");

		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	void saveMyGameReviewAllowsCompletedDroppedAndPausedStatuses() {
		ReviewRequest request = new ReviewRequest(null, "Body", null, null);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());
		when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

		for (LibraryStatus status : new LibraryStatus[] {LibraryStatus.COMPLETED, LibraryStatus.DROPPED, LibraryStatus.PAUSED}) {
			when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
					.thenReturn(Optional.of(libraryEntry(status)));

			assertThat(reviewService.saveMyGameReview(USER_ID, GAME_ID, request)).isNotNull();
		}
	}

	@Test
	void deleteMyGameReviewDeletesOwnersReview() {
		Review existing = review(ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC);

		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(existing));

		reviewService.deleteMyGameReview(USER_ID, GAME_ID);

		verify(reviewRepository).delete(existing);
	}

	@Test
	void deleteMyGameReviewRejectsMissingReview() {
		when(reviewRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.deleteMyGameReview(USER_ID, GAME_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Review not found");

		verify(reviewRepository, never()).delete(any(Review.class));
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

	private LibraryEntry eligibleEntry() {
		return libraryEntry(LibraryStatus.PLAYING);
	}

	private LibraryEntry libraryEntry(LibraryStatus status) {
		return new LibraryEntry(user(ProfileVisibility.PUBLIC), game(), status);
	}

	private Review review(ReviewVisibility visibility, ProfileVisibility profileVisibility) {
		Review review = new Review(user(profileVisibility), game(), "A public opinion.");
		review.setRating(8);
		review.setVisibility(visibility);
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);
		ReflectionTestUtils.setField(review, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(review, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));

		return review;
	}
}
