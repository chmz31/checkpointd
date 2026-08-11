package com.chmz31.checkpointd.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.comment.dto.CommentRequest;
import com.chmz31.checkpointd.comment.dto.CommentResponse;
import com.chmz31.checkpointd.comment.dto.ReportCommentRequest;
import com.chmz31.checkpointd.comment.dto.ReportedListCommentResponse;
import com.chmz31.checkpointd.comment.dto.ReportedReviewCommentResponse;
import com.chmz31.checkpointd.comment.entity.ListComment;
import com.chmz31.checkpointd.comment.entity.ListCommentReport;
import com.chmz31.checkpointd.comment.entity.ReviewComment;
import com.chmz31.checkpointd.comment.entity.ReviewCommentReport;
import com.chmz31.checkpointd.comment.repository.ListCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentRepository;
import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ForbiddenException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.entity.Game;
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
class CommentServiceTests {

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
	private ListCommentReportRepository listCommentReportRepository;

	@Mock
	private ReviewCommentReportRepository reviewCommentReportRepository;

	@Mock
	private GameListRepository gameListRepository;

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CommentService commentService;

	@Test
	void addListCommentCreatesComment() {
		User user = user(USER_ID);
		GameList list = list(ListVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.empty());
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list));
		when(listCommentRepository.save(any(ListComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CommentResponse response = commentService.addListComment(USER_ID, LIST_ID, new CommentRequest("  Nice list!  "));

		ArgumentCaptor<ListComment> captor = ArgumentCaptor.forClass(ListComment.class);
		verify(listCommentRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(user);
		assertThat(captor.getValue().getList()).isSameAs(list);
		assertThat(captor.getValue().getBody()).isEqualTo("Nice list!");
		assertThat(response.owner()).isTrue();
	}

	@Test
	void addListCommentRejectsInaccessibleList() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.empty());
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.addListComment(USER_ID, LIST_ID, new CommentRequest("Hi")))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("List not found");

		verify(listCommentRepository, never()).save(any(ListComment.class));
	}

	@Test
	void getListCommentsFlagsOwnerCorrectly() {
		GameList list = list(ListVisibility.PUBLIC);
		ListComment comment = listComment(list, user(USER_ID));

		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list));
		when(listCommentRepository.findByListIdOrderByCreatedAtDesc(eq(LIST_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(comment)));

		Page<CommentResponse> comments = commentService.getListComments(USER_ID, LIST_ID, 0, 20);

		assertThat(comments.getContent()).extracting(CommentResponse::owner).containsExactly(true);
	}

	@Test
	void getListCommentsWorksForAnonymousViewer() {
		GameList list = list(ListVisibility.PUBLIC);
		ListComment comment = listComment(list, user(USER_ID));

		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list));
		when(listCommentRepository.findByListIdOrderByCreatedAtDesc(eq(LIST_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(comment)));

		Page<CommentResponse> comments = commentService.getListComments(null, LIST_ID, 0, 20);

		assertThat(comments.getContent()).extracting(CommentResponse::owner).containsExactly(false);
	}

	@Test
	void deleteListCommentRemovesOwnComment() {
		ListComment comment = listComment(list(ListVisibility.PUBLIC), user(USER_ID));
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.of(comment));

		commentService.deleteListComment(USER_ID, false, LIST_ID, COMMENT_ID);

		verify(listCommentRepository).delete(comment);
	}

	@Test
	void deleteListCommentAllowsAdminToDeleteOthersComment() {
		ListComment comment = listComment(list(ListVisibility.PUBLIC), user(OTHER_USER_ID));
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.of(comment));

		commentService.deleteListComment(USER_ID, true, LIST_ID, COMMENT_ID);

		verify(listCommentRepository).delete(comment);
	}

	@Test
	void deleteListCommentRejectsNonOwnerNonAdmin() {
		ListComment comment = listComment(list(ListVisibility.PUBLIC), user(OTHER_USER_ID));
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.of(comment));

		assertThatThrownBy(() -> commentService.deleteListComment(USER_ID, false, LIST_ID, COMMENT_ID))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You can only delete your own comments");

		verify(listCommentRepository, never()).delete(any(ListComment.class));
	}

	@Test
	void deleteListCommentRejectsMissingComment() {
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.deleteListComment(USER_ID, false, LIST_ID, COMMENT_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Comment not found");
	}

	@Test
	void reportListCommentCreatesReport() {
		ListComment comment = listComment(list(ListVisibility.PUBLIC), user(OTHER_USER_ID));
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.of(comment));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(listCommentReportRepository.existsByCommentIdAndReporterId(COMMENT_ID, USER_ID)).thenReturn(false);

		commentService.reportListComment(USER_ID, LIST_ID, COMMENT_ID, new ReportCommentRequest("Spam"));

		ArgumentCaptor<ListCommentReport> captor = ArgumentCaptor.forClass(ListCommentReport.class);
		verify(listCommentReportRepository).save(captor.capture());
		assertThat(captor.getValue().getReason()).isEqualTo("Spam");
	}

	@Test
	void reportListCommentRejectsSelfReport() {
		ListComment comment = listComment(list(ListVisibility.PUBLIC), user(USER_ID));
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.of(comment));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));

		assertThatThrownBy(() -> commentService.reportListComment(USER_ID, LIST_ID, COMMENT_ID, new ReportCommentRequest(null)))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("You cannot report your own comment");

		verify(listCommentReportRepository, never()).save(any(ListCommentReport.class));
	}

	@Test
	void reportListCommentRejectsDuplicateReport() {
		ListComment comment = listComment(list(ListVisibility.PUBLIC), user(OTHER_USER_ID));
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.of(comment));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(listCommentReportRepository.existsByCommentIdAndReporterId(COMMENT_ID, USER_ID)).thenReturn(true);

		assertThatThrownBy(() -> commentService.reportListComment(USER_ID, LIST_ID, COMMENT_ID, new ReportCommentRequest(null)))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("You already reported this comment");
	}

	@Test
	void getReportedListCommentsRejectsNonAdmin() {
		assertThatThrownBy(() -> commentService.getReportedListComments(false, 0, 20))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("Admin access required");
	}

	@Test
	void getReportedListCommentsReturnsReportedComments() {
		ListComment comment = listComment(list(ListVisibility.PUBLIC), user(OTHER_USER_ID));
		when(listCommentRepository.findReportedOrderByCreatedAtDesc(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(comment)));
		when(listCommentReportRepository.countByCommentId(COMMENT_ID)).thenReturn(2L);

		Page<ReportedListCommentResponse> reported = commentService.getReportedListComments(true, 0, 20);

		assertThat(reported.getContent()).extracting(ReportedListCommentResponse::reportCount).containsExactly(2L);
		assertThat(reported.getContent()).extracting(ReportedListCommentResponse::listId).containsExactly(LIST_ID);
	}

	@Test
	void addReviewCommentCreatesComment() {
		User user = user(USER_ID);
		Review review = review(ReviewVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.empty());
		when(reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				REVIEW_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(review));
		when(reviewCommentRepository.save(any(ReviewComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CommentResponse response = commentService.addReviewComment(USER_ID, REVIEW_ID, new CommentRequest("Great review"));

		ArgumentCaptor<ReviewComment> captor = ArgumentCaptor.forClass(ReviewComment.class);
		verify(reviewCommentRepository).save(captor.capture());
		assertThat(captor.getValue().getReview()).isSameAs(review);
		assertThat(response.owner()).isTrue();
	}

	@Test
	void deleteReviewCommentRejectsNonOwnerNonAdmin() {
		ReviewComment comment = reviewComment(review(ReviewVisibility.PUBLIC), user(OTHER_USER_ID));
		when(reviewCommentRepository.findByIdAndReviewId(COMMENT_ID, REVIEW_ID)).thenReturn(Optional.of(comment));

		assertThatThrownBy(() -> commentService.deleteReviewComment(USER_ID, false, REVIEW_ID, COMMENT_ID))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You can only delete your own comments");
	}

	@Test
	void deleteReviewCommentAllowsAdminToDeleteOthersComment() {
		ReviewComment comment = reviewComment(review(ReviewVisibility.PUBLIC), user(OTHER_USER_ID));
		when(reviewCommentRepository.findByIdAndReviewId(COMMENT_ID, REVIEW_ID)).thenReturn(Optional.of(comment));

		commentService.deleteReviewComment(USER_ID, true, REVIEW_ID, COMMENT_ID);

		verify(reviewCommentRepository).delete(comment);
	}

	@Test
	void reportReviewCommentRejectsDuplicateReport() {
		ReviewComment comment = reviewComment(review(ReviewVisibility.PUBLIC), user(OTHER_USER_ID));
		when(reviewCommentRepository.findByIdAndReviewId(COMMENT_ID, REVIEW_ID)).thenReturn(Optional.of(comment));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(reviewCommentReportRepository.existsByCommentIdAndReporterId(COMMENT_ID, USER_ID)).thenReturn(true);

		assertThatThrownBy(() -> commentService.reportReviewComment(USER_ID, REVIEW_ID, COMMENT_ID, new ReportCommentRequest(null)))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("You already reported this comment");
	}

	@Test
	void getReportedReviewCommentsRejectsNonAdmin() {
		assertThatThrownBy(() -> commentService.getReportedReviewComments(false, 0, 20))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("Admin access required");
	}

	@Test
	void getReportedReviewCommentsReturnsReportedComments() {
		ReviewComment comment = reviewComment(review(ReviewVisibility.PUBLIC), user(OTHER_USER_ID));
		when(reviewCommentRepository.findReportedOrderByCreatedAtDesc(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(comment)));
		when(reviewCommentReportRepository.countByCommentId(COMMENT_ID)).thenReturn(1L);

		Page<ReportedReviewCommentResponse> reported = commentService.getReportedReviewComments(true, 0, 20);

		assertThat(reported.getContent()).extracting(ReportedReviewCommentResponse::reportCount).containsExactly(1L);
		assertThat(reported.getContent()).extracting(ReportedReviewCommentResponse::reviewId).containsExactly(REVIEW_ID);
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

	private ListComment listComment(GameList list, User author) {
		ListComment comment = new ListComment(author, list, "Great list!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}

	private ReviewComment reviewComment(Review review, User author) {
		ReviewComment comment = new ReviewComment(author, review, "Great review!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}
}
