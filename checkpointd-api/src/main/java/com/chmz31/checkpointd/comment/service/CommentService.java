package com.chmz31.checkpointd.comment.service;

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
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 50;

	private final ListCommentRepository listCommentRepository;
	private final ReviewCommentRepository reviewCommentRepository;
	private final ListCommentReportRepository listCommentReportRepository;
	private final ReviewCommentReportRepository reviewCommentReportRepository;
	private final GameListRepository gameListRepository;
	private final ReviewRepository reviewRepository;
	private final UserRepository userRepository;

	public CommentService(
			ListCommentRepository listCommentRepository,
			ReviewCommentRepository reviewCommentRepository,
			ListCommentReportRepository listCommentReportRepository,
			ReviewCommentReportRepository reviewCommentReportRepository,
			GameListRepository gameListRepository,
			ReviewRepository reviewRepository,
			UserRepository userRepository) {
		this.listCommentRepository = listCommentRepository;
		this.reviewCommentRepository = reviewCommentRepository;
		this.listCommentReportRepository = listCommentReportRepository;
		this.reviewCommentReportRepository = reviewCommentReportRepository;
		this.gameListRepository = gameListRepository;
		this.reviewRepository = reviewRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public CommentResponse addListComment(UUID userId, UUID listId, CommentRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		GameList list = accessibleList(userId, listId);

		ListComment saved = listCommentRepository.save(new ListComment(user, list, request.body().trim()));
		return CommentResponse.from(saved, true);
	}

	@Transactional(readOnly = true)
	public Page<CommentResponse> getListComments(UUID currentUserId, UUID listId, int page, int size) {
		accessibleList(currentUserId, listId);
		return listCommentRepository.findByListIdOrderByCreatedAtDesc(listId, pageRequest(page, size))
				.map(comment -> CommentResponse.from(comment, currentUserId != null && comment.getUser().getId().equals(currentUserId)));
	}

	@Transactional
	public void deleteListComment(UUID userId, boolean isAdmin, UUID listId, UUID commentId) {
		ListComment comment = listCommentRepository.findByIdAndListId(commentId, listId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
		requireOwnerOrAdmin(comment.getUser().getId(), userId, isAdmin);
		listCommentRepository.delete(comment);
	}

	@Transactional
	public void reportListComment(UUID userId, UUID listId, UUID commentId, ReportCommentRequest request) {
		ListComment comment = listCommentRepository.findByIdAndListId(commentId, listId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
		User reporter = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		requireNotSelfReport(comment.getUser().getId(), userId);

		if (listCommentReportRepository.existsByCommentIdAndReporterId(commentId, userId)) {
			throw new DuplicateResourceException("You already reported this comment");
		}

		listCommentReportRepository.save(new ListCommentReport(comment, reporter, cleanReason(request.reason())));
	}

	@Transactional(readOnly = true)
	public Page<ReportedListCommentResponse> getReportedListComments(boolean isAdmin, int page, int size) {
		requireAdmin(isAdmin);
		return listCommentRepository.findReportedOrderByCreatedAtDesc(pageRequest(page, size))
				.map(comment -> ReportedListCommentResponse.from(
						comment, listCommentReportRepository.countByCommentId(comment.getId())));
	}

	@Transactional
	public CommentResponse addReviewComment(UUID userId, UUID reviewId, CommentRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Review review = accessibleReview(userId, reviewId);

		ReviewComment saved = reviewCommentRepository.save(new ReviewComment(user, review, request.body().trim()));
		return CommentResponse.from(saved, true);
	}

	@Transactional(readOnly = true)
	public Page<CommentResponse> getReviewComments(UUID currentUserId, UUID reviewId, int page, int size) {
		accessibleReview(currentUserId, reviewId);
		return reviewCommentRepository.findByReviewIdOrderByCreatedAtDesc(reviewId, pageRequest(page, size))
				.map(comment -> CommentResponse.from(comment, currentUserId != null && comment.getUser().getId().equals(currentUserId)));
	}

	@Transactional
	public void deleteReviewComment(UUID userId, boolean isAdmin, UUID reviewId, UUID commentId) {
		ReviewComment comment = reviewCommentRepository.findByIdAndReviewId(commentId, reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
		requireOwnerOrAdmin(comment.getUser().getId(), userId, isAdmin);
		reviewCommentRepository.delete(comment);
	}

	@Transactional
	public void reportReviewComment(UUID userId, UUID reviewId, UUID commentId, ReportCommentRequest request) {
		ReviewComment comment = reviewCommentRepository.findByIdAndReviewId(commentId, reviewId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
		User reporter = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		requireNotSelfReport(comment.getUser().getId(), userId);

		if (reviewCommentReportRepository.existsByCommentIdAndReporterId(commentId, userId)) {
			throw new DuplicateResourceException("You already reported this comment");
		}

		reviewCommentReportRepository.save(new ReviewCommentReport(comment, reporter, cleanReason(request.reason())));
	}

	@Transactional(readOnly = true)
	public Page<ReportedReviewCommentResponse> getReportedReviewComments(boolean isAdmin, int page, int size) {
		requireAdmin(isAdmin);
		return reviewCommentRepository.findReportedOrderByCreatedAtDesc(pageRequest(page, size))
				.map(comment -> ReportedReviewCommentResponse.from(
						comment, reviewCommentReportRepository.countByCommentId(comment.getId())));
	}

	private GameList accessibleList(UUID currentUserId, UUID listId) {
		if (currentUserId != null) {
			var owned = gameListRepository.findByIdAndUserId(listId, currentUserId);
			if (owned.isPresent()) {
				return owned.get();
			}
		}
		return gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				listId, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)
				.orElseThrow(() -> new ResourceNotFoundException("List not found"));
	}

	private Review accessibleReview(UUID currentUserId, UUID reviewId) {
		if (currentUserId != null) {
			var owned = reviewRepository.findByIdAndUserId(reviewId, currentUserId);
			if (owned.isPresent()) {
				return owned.get();
			}
		}
		return reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				reviewId, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)
				.orElseThrow(() -> new ResourceNotFoundException("Review not found"));
	}

	private void requireOwnerOrAdmin(UUID ownerId, UUID currentUserId, boolean isAdmin) {
		if (!ownerId.equals(currentUserId) && !isAdmin) {
			throw new ForbiddenException("You can only delete your own comments");
		}
	}

	private void requireNotSelfReport(UUID commentOwnerId, UUID currentUserId) {
		if (commentOwnerId.equals(currentUserId)) {
			throw new BadRequestException("You cannot report your own comment");
		}
	}

	private void requireAdmin(boolean isAdmin) {
		if (!isAdmin) {
			throw new ForbiddenException("Admin access required");
		}
	}

	private String cleanReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}
		return reason.trim();
	}

	private PageRequest pageRequest(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize);
	}
}
