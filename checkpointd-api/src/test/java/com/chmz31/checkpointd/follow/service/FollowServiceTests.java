package com.chmz31.checkpointd.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class FollowServiceTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Mock
	private FollowRepository followRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private FollowService followService;

	@Test
	void followCreatesFollowRelationship() {
		User follower = user(USER_ID, "playerone", ProfileVisibility.PUBLIC);
		User followee = user(OTHER_USER_ID, "playertwo", ProfileVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(follower));
		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(followee));
		when(followRepository.existsByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(false);
		when(followRepository.countByFolloweeId(OTHER_USER_ID)).thenReturn(1L);
		when(followRepository.countByFollowerId(OTHER_USER_ID)).thenReturn(0L);

		FollowStatusResponse response = followService.follow(USER_ID, "playertwo");

		ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
		verify(followRepository).save(captor.capture());
		assertThat(captor.getValue().getFollower()).isSameAs(follower);
		assertThat(captor.getValue().getFollowee()).isSameAs(followee);
		assertThat(response.following()).isTrue();
		assertThat(response.followerCount()).isEqualTo(1L);
	}

	@Test
	void followRejectsSelfFollow() {
		User user = user(USER_ID, "playerone", ProfileVisibility.PUBLIC);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> followService.follow(USER_ID, "playerone"))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("You cannot follow yourself");

		verify(followRepository, never()).save(any(Follow.class));
	}

	@Test
	void followRejectsDuplicateFollow() {
		User follower = user(USER_ID, "playerone", ProfileVisibility.PUBLIC);
		User followee = user(OTHER_USER_ID, "playertwo", ProfileVisibility.PUBLIC);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(follower));
		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(followee));
		when(followRepository.existsByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(true);

		assertThatThrownBy(() -> followService.follow(USER_ID, "playertwo"))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("You are already following this user");

		verify(followRepository, never()).save(any(Follow.class));
	}

	@Test
	void followRejectsMissingTargetUser() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "playerone", ProfileVisibility.PUBLIC)));
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> followService.follow(USER_ID, "missing"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User not found");
	}

	@Test
	void unfollowRemovesExistingFollow() {
		User followee = user(OTHER_USER_ID, "playertwo", ProfileVisibility.PUBLIC);
		Follow follow = new Follow(user(USER_ID, "playerone", ProfileVisibility.PUBLIC), followee);

		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(followee));
		when(followRepository.findByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(Optional.of(follow));
		when(followRepository.countByFolloweeId(OTHER_USER_ID)).thenReturn(0L);
		when(followRepository.countByFollowerId(OTHER_USER_ID)).thenReturn(0L);

		FollowStatusResponse response = followService.unfollow(USER_ID, "playertwo");

		verify(followRepository).delete(follow);
		assertThat(response.following()).isFalse();
	}

	@Test
	void unfollowRejectsMissingFollow() {
		User followee = user(OTHER_USER_ID, "playertwo", ProfileVisibility.PUBLIC);
		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(followee));
		when(followRepository.findByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> followService.unfollow(USER_ID, "playertwo"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Follow not found");
	}

	@Test
	void getStatusReflectsCurrentFollowState() {
		User followee = user(OTHER_USER_ID, "playertwo", ProfileVisibility.PUBLIC);
		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(followee));
		when(followRepository.existsByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(true);
		when(followRepository.countByFolloweeId(OTHER_USER_ID)).thenReturn(3L);
		when(followRepository.countByFollowerId(OTHER_USER_ID)).thenReturn(2L);

		FollowStatusResponse response = followService.getStatus(USER_ID, "playertwo");

		assertThat(response.following()).isTrue();
		assertThat(response.followerCount()).isEqualTo(3L);
		assertThat(response.followingCount()).isEqualTo(2L);
	}

	@Test
	void getFollowersRejectsPrivateProfile() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(USER_ID, "playerone", ProfileVisibility.PRIVATE)));

		assertThatThrownBy(() -> followService.getFollowers("playerone", 0, 20))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Profile not found");
	}

	@Test
	void getFollowersReturnsFollowerSummaries() {
		User target = user(USER_ID, "playerone", ProfileVisibility.PUBLIC);
		User follower = user(OTHER_USER_ID, "playertwo", ProfileVisibility.PUBLIC);
		Follow follow = new Follow(follower, target);

		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(target));
		when(followRepository.findByFolloweeIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(follow)));

		Page<UserSummaryResponse> followers = followService.getFollowers("playerone", 0, 20);

		assertThat(followers.getContent()).extracting(UserSummaryResponse::username).containsExactly("playertwo");
	}

	@Test
	void getFollowingReturnsFolloweeSummaries() {
		User target = user(USER_ID, "playerone", ProfileVisibility.PUBLIC);
		User followee = user(OTHER_USER_ID, "playertwo", ProfileVisibility.PUBLIC);
		Follow follow = new Follow(target, followee);

		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(target));
		when(followRepository.findByFollowerIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(follow)));

		Page<UserSummaryResponse> following = followService.getFollowing("playerone", 0, 20);

		assertThat(following.getContent()).extracting(UserSummaryResponse::username).containsExactly("playertwo");
	}

	private User user(UUID id, String username, ProfileVisibility visibility) {
		User user = new User(username + "@example.com", username, "hash", Role.USER);
		user.setProfileVisibility(visibility);
		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}
}
