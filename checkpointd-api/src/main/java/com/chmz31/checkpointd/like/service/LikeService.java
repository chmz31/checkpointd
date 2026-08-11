package com.chmz31.checkpointd.like.service;

import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.like.dto.LikeStatusResponse;
import com.chmz31.checkpointd.like.entity.ListLike;
import com.chmz31.checkpointd.like.entity.ReviewLike;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.notification.service.NotificationService;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeService {

	private final ListLikeRepository listLikeRepository;
	private final ReviewLikeRepository reviewLikeRepository;
	private final UserRepository userRepository;
	private final GameListRepository gameListRepository;
	private final ReviewRepository reviewRepository;
	private final NotificationService notificationService;

	public LikeService(
			ListLikeRepository listLikeRepository,
			ReviewLikeRepository reviewLikeRepository,
			UserRepository userRepository,
			GameListRepository gameListRepository,
			ReviewRepository reviewRepository,
			NotificationService notificationService) {
		this.listLikeRepository = listLikeRepository;
		this.reviewLikeRepository = reviewLikeRepository;
		this.userRepository = userRepository;
		this.gameListRepository = gameListRepository;
		this.reviewRepository = reviewRepository;
		this.notificationService = notificationService;
	}

	@Transactional
	public LikeStatusResponse likeList(UUID currentUserId, UUID listId) {
		User user = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		GameList list = accessibleList(currentUserId, listId);

		if (listLikeRepository.existsByUserIdAndListId(currentUserId, list.getId())) {
			throw new DuplicateResourceException("You already liked this list");
		}

		listLikeRepository.save(new ListLike(user, list));
		notificationService.notifyListLiked(user, list);
		return listStatus(currentUserId, list.getId());
	}

	@Transactional
	public LikeStatusResponse unlikeList(UUID currentUserId, UUID listId) {
		GameList list = accessibleList(currentUserId, listId);
		ListLike like = listLikeRepository.findByUserIdAndListId(currentUserId, list.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Like not found"));

		listLikeRepository.delete(like);
		return listStatus(currentUserId, list.getId());
	}

	@Transactional(readOnly = true)
	public LikeStatusResponse getListLikeStatus(UUID currentUserId, UUID listId) {
		GameList list = accessibleList(currentUserId, listId);
		return listStatus(currentUserId, list.getId());
	}

	@Transactional
	public LikeStatusResponse likeReview(UUID currentUserId, UUID reviewId) {
		User user = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Review review = accessibleReview(currentUserId, reviewId);

		if (reviewLikeRepository.existsByUserIdAndReviewId(currentUserId, review.getId())) {
			throw new DuplicateResourceException("You already liked this review");
		}

		reviewLikeRepository.save(new ReviewLike(user, review));
		notificationService.notifyReviewLiked(user, review);
		return reviewStatus(currentUserId, review.getId());
	}

	@Transactional
	public LikeStatusResponse unlikeReview(UUID currentUserId, UUID reviewId) {
		Review review = accessibleReview(currentUserId, reviewId);
		ReviewLike like = reviewLikeRepository.findByUserIdAndReviewId(currentUserId, review.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Like not found"));

		reviewLikeRepository.delete(like);
		return reviewStatus(currentUserId, review.getId());
	}

	@Transactional(readOnly = true)
	public LikeStatusResponse getReviewLikeStatus(UUID currentUserId, UUID reviewId) {
		Review review = accessibleReview(currentUserId, reviewId);
		return reviewStatus(currentUserId, review.getId());
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

	private LikeStatusResponse listStatus(UUID currentUserId, UUID listId) {
		boolean liked = currentUserId != null && listLikeRepository.existsByUserIdAndListId(currentUserId, listId);
		return new LikeStatusResponse(liked, listLikeRepository.countByListId(listId));
	}

	private LikeStatusResponse reviewStatus(UUID currentUserId, UUID reviewId) {
		boolean liked = currentUserId != null && reviewLikeRepository.existsByUserIdAndReviewId(currentUserId, reviewId);
		return new LikeStatusResponse(liked, reviewLikeRepository.countByReviewId(reviewId));
	}
}
