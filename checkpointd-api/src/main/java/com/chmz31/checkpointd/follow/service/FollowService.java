package com.chmz31.checkpointd.follow.service;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.follow.dto.FollowStatusResponse;
import com.chmz31.checkpointd.follow.dto.UserSummaryResponse;
import com.chmz31.checkpointd.follow.entity.Follow;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.notification.service.NotificationService;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 50;

	private final FollowRepository followRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	public FollowService(
			FollowRepository followRepository, UserRepository userRepository, NotificationService notificationService) {
		this.followRepository = followRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
	}

	@Transactional
	public FollowStatusResponse follow(UUID currentUserId, String targetUsername) {
		User follower = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		User followee = userRepository.findByUsername(targetUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (follower.getId().equals(followee.getId())) {
			throw new BadRequestException("You cannot follow yourself");
		}
		if (followRepository.existsByFollowerIdAndFolloweeId(follower.getId(), followee.getId())) {
			throw new DuplicateResourceException("You are already following this user");
		}

		followRepository.save(new Follow(follower, followee));
		notificationService.notifyFollow(follower, followee);
		return status(followee.getId(), true);
	}

	@Transactional
	public FollowStatusResponse unfollow(UUID currentUserId, String targetUsername) {
		User followee = userRepository.findByUsername(targetUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Follow follow = followRepository.findByFollowerIdAndFolloweeId(currentUserId, followee.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Follow not found"));

		followRepository.delete(follow);
		return status(followee.getId(), false);
	}

	@Transactional(readOnly = true)
	public FollowStatusResponse getStatus(UUID currentUserId, String targetUsername) {
		User followee = userRepository.findByUsername(targetUsername)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return status(followee.getId(), followRepository.existsByFollowerIdAndFolloweeId(currentUserId, followee.getId()));
	}

	@Transactional(readOnly = true)
	public Page<UserSummaryResponse> getFollowers(String username, int page, int size) {
		User user = publicUser(username);
		return followRepository.findByFolloweeIdOrderByCreatedAtDesc(user.getId(), pageRequest(page, size))
				.map(follow -> UserSummaryResponse.from(follow.getFollower()));
	}

	@Transactional(readOnly = true)
	public Page<UserSummaryResponse> getFollowing(String username, int page, int size) {
		User user = publicUser(username);
		return followRepository.findByFollowerIdOrderByCreatedAtDesc(user.getId(), pageRequest(page, size))
				.map(follow -> UserSummaryResponse.from(follow.getFollowee()));
	}

	private FollowStatusResponse status(UUID followeeId, boolean following) {
		return new FollowStatusResponse(
				following,
				followRepository.countByFolloweeId(followeeId),
				followRepository.countByFollowerId(followeeId));
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
