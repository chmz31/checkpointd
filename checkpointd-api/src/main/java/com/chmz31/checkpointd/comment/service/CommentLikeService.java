package com.chmz31.checkpointd.comment.service;

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
import com.chmz31.checkpointd.like.dto.LikeStatusResponse;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentLikeService {

	private final ListCommentRepository listCommentRepository;
	private final ReviewCommentRepository reviewCommentRepository;
	private final ListCommentLikeRepository listCommentLikeRepository;
	private final ReviewCommentLikeRepository reviewCommentLikeRepository;
	private final UserRepository userRepository;

	public CommentLikeService(
			ListCommentRepository listCommentRepository,
			ReviewCommentRepository reviewCommentRepository,
			ListCommentLikeRepository listCommentLikeRepository,
			ReviewCommentLikeRepository reviewCommentLikeRepository,
			UserRepository userRepository) {
		this.listCommentRepository = listCommentRepository;
		this.reviewCommentRepository = reviewCommentRepository;
		this.listCommentLikeRepository = listCommentLikeRepository;
		this.reviewCommentLikeRepository = reviewCommentLikeRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public LikeStatusResponse likeListComment(UUID currentUserId, UUID commentId) {
		User user = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		ListComment comment = accessibleListComment(currentUserId, commentId);

		if (listCommentLikeRepository.existsByUserIdAndCommentId(currentUserId, commentId)) {
			throw new DuplicateResourceException("You already liked this comment");
		}

		listCommentLikeRepository.save(new ListCommentLike(user, comment));
		return listCommentStatus(currentUserId, commentId);
	}

	@Transactional
	public LikeStatusResponse unlikeListComment(UUID currentUserId, UUID commentId) {
		accessibleListComment(currentUserId, commentId);
		ListCommentLike like = listCommentLikeRepository.findByUserIdAndCommentId(currentUserId, commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Like not found"));

		listCommentLikeRepository.delete(like);
		return listCommentStatus(currentUserId, commentId);
	}

	@Transactional(readOnly = true)
	public LikeStatusResponse getListCommentLikeStatus(UUID currentUserId, UUID commentId) {
		accessibleListComment(currentUserId, commentId);
		return listCommentStatus(currentUserId, commentId);
	}

	@Transactional
	public LikeStatusResponse likeReviewComment(UUID currentUserId, UUID commentId) {
		User user = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		ReviewComment comment = accessibleReviewComment(currentUserId, commentId);

		if (reviewCommentLikeRepository.existsByUserIdAndCommentId(currentUserId, commentId)) {
			throw new DuplicateResourceException("You already liked this comment");
		}

		reviewCommentLikeRepository.save(new ReviewCommentLike(user, comment));
		return reviewCommentStatus(currentUserId, commentId);
	}

	@Transactional
	public LikeStatusResponse unlikeReviewComment(UUID currentUserId, UUID commentId) {
		accessibleReviewComment(currentUserId, commentId);
		ReviewCommentLike like = reviewCommentLikeRepository.findByUserIdAndCommentId(currentUserId, commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Like not found"));

		reviewCommentLikeRepository.delete(like);
		return reviewCommentStatus(currentUserId, commentId);
	}

	@Transactional(readOnly = true)
	public LikeStatusResponse getReviewCommentLikeStatus(UUID currentUserId, UUID commentId) {
		accessibleReviewComment(currentUserId, commentId);
		return reviewCommentStatus(currentUserId, commentId);
	}

	private ListComment accessibleListComment(UUID currentUserId, UUID commentId) {
		ListComment comment = listCommentRepository.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
		if (!isAccessible(
				currentUserId,
				comment.getList().getUser().getId(),
				comment.getList().getVisibility() == ListVisibility.PUBLIC,
				comment.getList().getUser().getProfileVisibility() == ProfileVisibility.PUBLIC)) {
			throw new ResourceNotFoundException("Comment not found");
		}
		return comment;
	}

	private ReviewComment accessibleReviewComment(UUID currentUserId, UUID commentId) {
		ReviewComment comment = reviewCommentRepository.findById(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
		if (!isAccessible(
				currentUserId,
				comment.getReview().getUser().getId(),
				comment.getReview().getVisibility() == ReviewVisibility.PUBLIC,
				comment.getReview().getUser().getProfileVisibility() == ProfileVisibility.PUBLIC)) {
			throw new ResourceNotFoundException("Comment not found");
		}
		return comment;
	}

	private boolean isAccessible(UUID currentUserId, UUID ownerId, boolean targetPublic, boolean profilePublic) {
		boolean owned = currentUserId != null && ownerId.equals(currentUserId);
		return owned || (targetPublic && profilePublic);
	}

	private LikeStatusResponse listCommentStatus(UUID currentUserId, UUID commentId) {
		boolean liked = currentUserId != null && listCommentLikeRepository.existsByUserIdAndCommentId(currentUserId, commentId);
		return new LikeStatusResponse(liked, listCommentLikeRepository.countByCommentId(commentId));
	}

	private LikeStatusResponse reviewCommentStatus(UUID currentUserId, UUID commentId) {
		boolean liked = currentUserId != null && reviewCommentLikeRepository.existsByUserIdAndCommentId(currentUserId, commentId);
		return new LikeStatusResponse(liked, reviewCommentLikeRepository.countByCommentId(commentId));
	}
}
