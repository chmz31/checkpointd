package com.chmz31.checkpointd.profile.service;

import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.follow.dto.UserSummaryResponse;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.profile.dto.PublicProfileGameResponse;
import com.chmz31.checkpointd.profile.dto.PublicProfileResponse;
import com.chmz31.checkpointd.profile.dto.PublicProfileStatsResponse;
import com.chmz31.checkpointd.profile.dto.UpdateProfileRequest;
import com.chmz31.checkpointd.review.dto.ReviewResponse;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

	private static final int RECENT_REVIEWS_LIMIT = 3;
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 50;

	private final UserRepository userRepository;
	private final LibraryEntryRepository libraryEntryRepository;
	private final ReviewRepository reviewRepository;
	private final ReviewLikeRepository reviewLikeRepository;
	private final FollowRepository followRepository;

	public ProfileService(
			UserRepository userRepository,
			LibraryEntryRepository libraryEntryRepository,
			ReviewRepository reviewRepository,
			ReviewLikeRepository reviewLikeRepository,
			FollowRepository followRepository) {
		this.userRepository = userRepository;
		this.libraryEntryRepository = libraryEntryRepository;
		this.reviewRepository = reviewRepository;
		this.reviewLikeRepository = reviewLikeRepository;
		this.followRepository = followRepository;
	}

	@Transactional(readOnly = true)
	public PublicProfileResponse getPublicProfile(String username, UUID currentUserId) {
		User user = findByUsername(username);
		if (user.getProfileVisibility() != ProfileVisibility.PUBLIC) {
			throw new ResourceNotFoundException("Profile not found");
		}

		return buildProfile(user, currentUserId);
	}

	@Transactional(readOnly = true)
	public PublicProfileResponse getMyProfile(UUID userId) {
		return buildProfile(findById(userId), userId);
	}

	@Transactional
	public PublicProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
		User user = findById(userId);
		user.setDisplayName(blankToNull(request.displayName()));
		user.setBio(blankToNull(request.bio()));
		if (request.profileVisibility() != null) {
			user.setProfileVisibility(request.profileVisibility());
		}

		return buildProfile(userRepository.save(user), userId);
	}

	private PublicProfileResponse buildProfile(User user, UUID currentUserId) {
		UUID userId = user.getId();
		String username = user.getUsername();
		long reviewCount = reviewRepository.countByUserUsernameAndUserProfileVisibilityAndVisibility(
				username, ProfileVisibility.PUBLIC, ReviewVisibility.PUBLIC);
		var stats = new PublicProfileStatsResponse(
				libraryEntryRepository.countByUserId(userId),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.COMPLETED),
				reviewRepository.countByUserUsernameAndUserProfileVisibilityAndVisibilityAndRatingIsNotNull(
						username, ProfileVisibility.PUBLIC, ReviewVisibility.PUBLIC),
				reviewRepository.averageRatingByUsernameAndVisibility(
						username, ProfileVisibility.PUBLIC, ReviewVisibility.PUBLIC),
				reviewCount,
				followRepository.countByFolloweeId(userId),
				followRepository.countByFollowerId(userId));
		var recentGames = libraryEntryRepository.findTop8ByUserIdOrderByUpdatedAtDesc(userId).stream()
				.map(PublicProfileGameResponse::from)
				.toList();
		var recentReviews = reviewRepository.findByUserUsernameAndUserProfileVisibilityAndVisibility(
				username, ProfileVisibility.PUBLIC, ReviewVisibility.PUBLIC,
				PageRequest.of(0, RECENT_REVIEWS_LIMIT, Sort.by(Sort.Direction.DESC, "updatedAt")))
				.map(review -> {
					long likeCount = reviewLikeRepository.countByReviewId(review.getId());
					boolean liked = currentUserId != null
							&& reviewLikeRepository.existsByUserIdAndReviewId(currentUserId, review.getId());
					return ReviewResponse.from(review, likeCount, liked, false);
				})
				.toList();

		return new PublicProfileResponse(
				user.getUsername(),
				user.getDisplayName(),
				user.getBio(),
				user.getProfileVisibility(),
				user.getCreatedAt(),
				stats,
				recentGames,
				recentReviews);
	}

	@Transactional(readOnly = true)
	public Page<UserSummaryResponse> searchUsers(String query, int page, int size) {
		return userRepository.searchPublicUsers(query == null ? "" : query.trim(), pageRequest(page, size))
				.map(UserSummaryResponse::from);
	}

	private User findByUsername(String username) {
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
	}

	private User findById(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private PageRequest pageRequest(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize);
	}
}
