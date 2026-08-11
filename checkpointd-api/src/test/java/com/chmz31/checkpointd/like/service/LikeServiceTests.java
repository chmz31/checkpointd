package com.chmz31.checkpointd.like.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.like.dto.LikeStatusResponse;
import com.chmz31.checkpointd.like.entity.ListLike;
import com.chmz31.checkpointd.like.entity.ReviewLike;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LikeServiceTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

	@Mock
	private ListLikeRepository listLikeRepository;

	@Mock
	private ReviewLikeRepository reviewLikeRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private GameListRepository gameListRepository;

	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private LikeService likeService;

	@Test
	void likeListCreatesLikeForAccessibleList() {
		User user = user(USER_ID);
		GameList list = list(ListVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.empty());
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list));
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(false, true);
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(1L);

		LikeStatusResponse response = likeService.likeList(USER_ID, LIST_ID);

		ArgumentCaptor<ListLike> captor = ArgumentCaptor.forClass(ListLike.class);
		verify(listLikeRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(user);
		assertThat(captor.getValue().getList()).isSameAs(list);
		assertThat(response.liked()).isTrue();
		assertThat(response.likeCount()).isEqualTo(1L);
	}

	@Test
	void likeListRejectsDuplicateLike() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(true);

		assertThatThrownBy(() -> likeService.likeList(USER_ID, LIST_ID))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("You already liked this list");

		verify(listLikeRepository, never()).save(any(ListLike.class));
	}

	@Test
	void likeListRejectsInaccessibleList() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.empty());
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> likeService.likeList(USER_ID, LIST_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("List not found");

		verify(listLikeRepository, never()).save(any(ListLike.class));
	}

	@Test
	void unlikeListRemovesExistingLike() {
		GameList list = list(ListVisibility.PUBLIC);
		ListLike like = new ListLike(user(USER_ID), list);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(listLikeRepository.findByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(Optional.of(like));
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(false);
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(0L);

		LikeStatusResponse response = likeService.unlikeList(USER_ID, LIST_ID);

		verify(listLikeRepository).delete(like);
		assertThat(response.liked()).isFalse();
	}

	@Test
	void unlikeListRejectsMissingLike() {
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(listLikeRepository.findByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> likeService.unlikeList(USER_ID, LIST_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Like not found");
	}

	@Test
	void getListLikeStatusReflectsCurrentUserLike() {
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(true);
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(4L);

		LikeStatusResponse response = likeService.getListLikeStatus(USER_ID, LIST_ID);

		assertThat(response.liked()).isTrue();
		assertThat(response.likeCount()).isEqualTo(4L);
	}

	@Test
	void getListLikeStatusForAnonymousUserIsNeverLiked() {
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(2L);

		LikeStatusResponse response = likeService.getListLikeStatus(null, LIST_ID);

		assertThat(response.liked()).isFalse();
		assertThat(response.likeCount()).isEqualTo(2L);
	}

	@Test
	void likeReviewCreatesLikeForAccessibleReview() {
		User user = user(USER_ID);
		Review review = review(ReviewVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.empty());
		when(reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				REVIEW_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(review));
		when(reviewLikeRepository.existsByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(false, true);
		when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(1L);

		LikeStatusResponse response = likeService.likeReview(USER_ID, REVIEW_ID);

		ArgumentCaptor<ReviewLike> captor = ArgumentCaptor.forClass(ReviewLike.class);
		verify(reviewLikeRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(user);
		assertThat(captor.getValue().getReview()).isSameAs(review);
		assertThat(response.liked()).isTrue();
		assertThat(response.likeCount()).isEqualTo(1L);
	}

	@Test
	void likeReviewRejectsDuplicateLike() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.of(review(ReviewVisibility.PUBLIC)));
		when(reviewLikeRepository.existsByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(true);

		assertThatThrownBy(() -> likeService.likeReview(USER_ID, REVIEW_ID))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("You already liked this review");

		verify(reviewLikeRepository, never()).save(any(ReviewLike.class));
	}

	@Test
	void likeReviewRejectsInaccessibleReview() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.empty());
		when(reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				REVIEW_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> likeService.likeReview(USER_ID, REVIEW_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Review not found");

		verify(reviewLikeRepository, never()).save(any(ReviewLike.class));
	}

	@Test
	void unlikeReviewRemovesExistingLike() {
		Review review = review(ReviewVisibility.PUBLIC);
		ReviewLike like = new ReviewLike(user(USER_ID), review);

		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.of(review));
		when(reviewLikeRepository.findByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(Optional.of(like));
		when(reviewLikeRepository.existsByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(false);
		when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(0L);

		LikeStatusResponse response = likeService.unlikeReview(USER_ID, REVIEW_ID);

		verify(reviewLikeRepository).delete(like);
		assertThat(response.liked()).isFalse();
	}

	@Test
	void unlikeReviewRejectsMissingLike() {
		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.of(review(ReviewVisibility.PUBLIC)));
		when(reviewLikeRepository.findByUserIdAndReviewId(USER_ID, REVIEW_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> likeService.unlikeReview(USER_ID, REVIEW_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Like not found");
	}

	@Test
	void getReviewLikeStatusForAnonymousUserIsNeverLiked() {
		when(reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				REVIEW_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(review(ReviewVisibility.PUBLIC)));
		when(reviewLikeRepository.countByReviewId(REVIEW_ID)).thenReturn(3L);

		LikeStatusResponse response = likeService.getReviewLikeStatus(null, REVIEW_ID);

		assertThat(response.liked()).isFalse();
		assertThat(response.likeCount()).isEqualTo(3L);
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
		ReflectionTestUtils.setField(game, "id", UUID.fromString("00000000-0000-0000-0000-000000000101"));

		Review review = new Review(user(OTHER_USER_ID), game, "A public opinion.");
		review.setVisibility(visibility);
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);

		return review;
	}
}
