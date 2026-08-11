package com.chmz31.checkpointd.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.comment.entity.ListComment;
import com.chmz31.checkpointd.comment.entity.ListCommentLike;
import com.chmz31.checkpointd.comment.entity.ReviewComment;
import com.chmz31.checkpointd.comment.entity.ReviewCommentLike;
import com.chmz31.checkpointd.comment.repository.ListCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentRepository;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.like.dto.LikeStatusResponse;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.notification.service.NotificationService;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
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
class CommentLikeServiceTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID COMMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Mock
	private ListCommentRepository listCommentRepository;

	@Mock
	private ReviewCommentRepository reviewCommentRepository;

	@Mock
	private ListCommentLikeRepository listCommentLikeRepository;

	@Mock
	private ReviewCommentLikeRepository reviewCommentLikeRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private CommentLikeService commentLikeService;

	@Test
	void likeListCommentCreatesLike() {
		User user = user(USER_ID);
		ListComment comment = listComment(ListVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
		when(listCommentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(false, true);
		when(listCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(1L);

		LikeStatusResponse response = commentLikeService.likeListComment(USER_ID, COMMENT_ID);

		ArgumentCaptor<ListCommentLike> captor = ArgumentCaptor.forClass(ListCommentLike.class);
		verify(listCommentLikeRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(user);
		assertThat(captor.getValue().getComment()).isSameAs(comment);
		assertThat(response.liked()).isTrue();
		assertThat(response.likeCount()).isEqualTo(1L);
	}

	@Test
	void likeListCommentRejectsDuplicateLike() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(listComment(ListVisibility.PUBLIC)));
		when(listCommentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(true);

		assertThatThrownBy(() -> commentLikeService.likeListComment(USER_ID, COMMENT_ID))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("You already liked this comment");

		verify(listCommentLikeRepository, never()).save(any(ListCommentLike.class));
	}

	@Test
	void likeListCommentRejectsInaccessibleComment() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(listComment(ListVisibility.PRIVATE)));

		assertThatThrownBy(() -> commentLikeService.likeListComment(USER_ID, COMMENT_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Comment not found");

		verify(listCommentLikeRepository, never()).save(any(ListCommentLike.class));
	}

	@Test
	void unlikeListCommentRemovesExistingLike() {
		ListComment comment = listComment(ListVisibility.PUBLIC);
		ListCommentLike like = new ListCommentLike(user(USER_ID), comment);

		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
		when(listCommentLikeRepository.findByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(Optional.of(like));
		when(listCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(0L);

		LikeStatusResponse response = commentLikeService.unlikeListComment(USER_ID, COMMENT_ID);

		verify(listCommentLikeRepository).delete(like);
		assertThat(response.liked()).isFalse();
	}

	@Test
	void unlikeListCommentRejectsMissingLike() {
		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(listComment(ListVisibility.PUBLIC)));
		when(listCommentLikeRepository.findByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentLikeService.unlikeListComment(USER_ID, COMMENT_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Like not found");
	}

	@Test
	void getListCommentLikeStatusForAnonymousUserIsNeverLiked() {
		when(listCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(listComment(ListVisibility.PUBLIC)));
		when(listCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(2L);

		LikeStatusResponse response = commentLikeService.getListCommentLikeStatus(null, COMMENT_ID);

		assertThat(response.liked()).isFalse();
		assertThat(response.likeCount()).isEqualTo(2L);
	}

	@Test
	void likeReviewCommentCreatesLike() {
		User user = user(USER_ID);
		ReviewComment comment = reviewComment(ReviewVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(reviewCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
		when(reviewCommentLikeRepository.existsByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(false, true);
		when(reviewCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(1L);

		LikeStatusResponse response = commentLikeService.likeReviewComment(USER_ID, COMMENT_ID);

		ArgumentCaptor<ReviewCommentLike> captor = ArgumentCaptor.forClass(ReviewCommentLike.class);
		verify(reviewCommentLikeRepository).save(captor.capture());
		assertThat(captor.getValue().getComment()).isSameAs(comment);
		assertThat(response.liked()).isTrue();
	}

	@Test
	void likeReviewCommentRejectsInaccessibleComment() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(reviewCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(reviewComment(ReviewVisibility.PRIVATE)));

		assertThatThrownBy(() -> commentLikeService.likeReviewComment(USER_ID, COMMENT_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Comment not found");
	}

	@Test
	void unlikeReviewCommentRemovesExistingLike() {
		ReviewComment comment = reviewComment(ReviewVisibility.PUBLIC);
		ReviewCommentLike like = new ReviewCommentLike(user(USER_ID), comment);

		when(reviewCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
		when(reviewCommentLikeRepository.findByUserIdAndCommentId(USER_ID, COMMENT_ID)).thenReturn(Optional.of(like));
		when(reviewCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(0L);

		LikeStatusResponse response = commentLikeService.unlikeReviewComment(USER_ID, COMMENT_ID);

		verify(reviewCommentLikeRepository).delete(like);
		assertThat(response.liked()).isFalse();
	}

	@Test
	void getReviewCommentLikeStatusForAnonymousUserIsNeverLiked() {
		when(reviewCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(reviewComment(ReviewVisibility.PUBLIC)));
		when(reviewCommentLikeRepository.countByCommentId(COMMENT_ID)).thenReturn(3L);

		LikeStatusResponse response = commentLikeService.getReviewCommentLikeStatus(null, COMMENT_ID);

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
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		Review review = new Review(user(OTHER_USER_ID), game, "A public opinion.");
		review.setVisibility(visibility);
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);

		return review;
	}

	private ListComment listComment(ListVisibility visibility) {
		ListComment comment = new ListComment(user(OTHER_USER_ID), list(visibility), "Great list!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}

	private ReviewComment reviewComment(ReviewVisibility visibility) {
		ReviewComment comment = new ReviewComment(user(OTHER_USER_ID), review(visibility), "Great review!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}
}
