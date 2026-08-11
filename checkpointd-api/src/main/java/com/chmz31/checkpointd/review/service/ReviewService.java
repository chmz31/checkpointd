package com.chmz31.checkpointd.review.service;

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
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int MAX_PAGE_SIZE = 50;
	private static final Set<LibraryStatus> REVIEWABLE_STATUSES = EnumSet.of(
			LibraryStatus.PLAYING, LibraryStatus.COMPLETED, LibraryStatus.DROPPED, LibraryStatus.PAUSED);

	private final ReviewRepository reviewRepository;
	private final ReviewLikeRepository reviewLikeRepository;
	private final UserRepository userRepository;
	private final GameRepository gameRepository;
	private final LibraryEntryRepository libraryEntryRepository;

	public ReviewService(
			ReviewRepository reviewRepository,
			ReviewLikeRepository reviewLikeRepository,
			UserRepository userRepository,
			GameRepository gameRepository,
			LibraryEntryRepository libraryEntryRepository) {
		this.reviewRepository = reviewRepository;
		this.reviewLikeRepository = reviewLikeRepository;
		this.userRepository = userRepository;
		this.gameRepository = gameRepository;
		this.libraryEntryRepository = libraryEntryRepository;
	}

	@Transactional(readOnly = true)
	public Page<ReviewResponse> getPublicGameReviews(UUID gameId, UUID currentUserId, int page, int size) {
		return reviewRepository.findByGameIdAndVisibilityAndUserProfileVisibilityOrderByUpdatedAtDesc(
				gameId,
				ReviewVisibility.PUBLIC,
				ProfileVisibility.PUBLIC,
				pageRequest(page, size))
				.map(review -> toResponse(review, currentUserId, false));
	}

	@Transactional(readOnly = true)
	public Page<ReviewResponse> getPublicUserReviews(String username, UUID currentUserId, int page, int size) {
		User user = publicUser(username);
		return reviewRepository.findByUserUsernameAndUserProfileVisibilityAndVisibilityOrderByUpdatedAtDesc(
				user.getUsername(),
				ProfileVisibility.PUBLIC,
				ReviewVisibility.PUBLIC,
				pageRequest(page, size))
				.map(review -> toResponse(review, currentUserId, false));
	}

	@Transactional(readOnly = true)
	public ReviewResponse getPublicUserGameReview(String username, UUID gameId, UUID currentUserId) {
		publicUser(username);
		return reviewRepository.findByUserUsernameAndGameIdAndVisibilityAndUserProfileVisibility(
				username,
				gameId,
				ReviewVisibility.PUBLIC,
				ProfileVisibility.PUBLIC)
				.map(review -> toResponse(review, currentUserId, false))
				.orElseThrow(() -> new ResourceNotFoundException("Review not found"));
	}

	@Transactional(readOnly = true)
	public Page<ReviewResponse> getMyReviews(UUID userId, int page, int size) {
		return reviewRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageRequest(page, size))
				.map(review -> toResponse(review, userId, true));
	}

	@Transactional(readOnly = true)
	public ReviewResponse getMyGameReview(UUID userId, UUID gameId) {
		return reviewRepository.findByUserIdAndGameId(userId, gameId)
				.map(review -> toResponse(review, userId, true))
				.orElseThrow(() -> new ResourceNotFoundException("Review not found"));
	}

	@Transactional
	public ReviewResponse saveMyGameReview(UUID userId, UUID gameId, ReviewRequest request) {
		requireReviewEligibility(userId, gameId);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new ResourceNotFoundException("Game not found"));
		Review review = reviewRepository.findByUserIdAndGameId(userId, gameId)
				.orElseGet(() -> new Review(user, game, request.body().trim()));

		review.setBody(request.body().trim());
		review.setRating(request.rating());
		review.setContainsSpoilers(Boolean.TRUE.equals(request.containsSpoilers()));
		review.setVisibility(request.visibility() == null ? ReviewVisibility.PUBLIC : request.visibility());

		return toResponse(reviewRepository.save(review), userId, true);
	}

	@Transactional
	public void deleteMyGameReview(UUID userId, UUID gameId) {
		Review review = reviewRepository.findByUserIdAndGameId(userId, gameId)
				.orElseThrow(() -> new ResourceNotFoundException("Review not found"));
		reviewRepository.delete(review);
	}

	private ReviewResponse toResponse(Review review, UUID currentUserId, boolean owner) {
		long likeCount = reviewLikeRepository.countByReviewId(review.getId());
		boolean liked = currentUserId != null
				&& reviewLikeRepository.existsByUserIdAndReviewId(currentUserId, review.getId());
		return ReviewResponse.from(review, likeCount, liked, owner);
	}

	private void requireReviewEligibility(UUID userId, UUID gameId) {
		LibraryEntry entry = libraryEntryRepository.findByUserIdAndGameId(userId, gameId)
				.orElseThrow(() -> new BadRequestException("Add this game to your library before reviewing it"));
		if (!REVIEWABLE_STATUSES.contains(entry.getStatus())) {
			throw new BadRequestException("You can only review games you have started playing");
		}
	}

	private User publicUser(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
		if (user.getProfileVisibility() != ProfileVisibility.PUBLIC) {
			throw new ResourceNotFoundException("Profile not found");
		}
		return user;
	}

	private PageRequest pageRequest(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize);
	}
}
