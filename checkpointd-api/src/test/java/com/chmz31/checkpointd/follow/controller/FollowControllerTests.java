package com.chmz31.checkpointd.follow.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.follow.entity.Follow;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FollowControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FollowRepository followRepository;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private ReviewRepository reviewRepository;

	@MockitoBean
	private GameListRepository gameListRepository;

	@MockitoBean
	private GameListItemRepository gameListItemRepository;

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

	@MockitoBean
	private ListLikeRepository listLikeRepository;

	@MockitoBean
	private ReviewLikeRepository reviewLikeRepository;

	@Test
	void followRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/follows/users/playertwo").with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanFollow() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "playerone")));
		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(user(OTHER_USER_ID, "playertwo")));
		when(followRepository.existsByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(false);
		when(followRepository.countByFolloweeId(OTHER_USER_ID)).thenReturn(1L);
		when(followRepository.countByFollowerId(OTHER_USER_ID)).thenReturn(0L);

		mockMvc.perform(post("/api/v1/follows/users/playertwo")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.following").value(true))
				.andExpect(jsonPath("$.followerCount").value(1));
	}

	@Test
	void followingSelfReturnsBadRequest() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, "playerone")));
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(USER_ID, "playerone")));

		mockMvc.perform(post("/api/v1/follows/users/playerone")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("You cannot follow yourself"));
	}

	@Test
	void unfollowRequiresAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/follows/users/playertwo").with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanUnfollow() throws Exception {
		User follower = user(USER_ID, "playerone");
		User followee = user(OTHER_USER_ID, "playertwo");
		Follow follow = new Follow(follower, followee);

		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(followee));
		when(followRepository.findByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(Optional.of(follow));

		mockMvc.perform(delete("/api/v1/follows/users/playertwo")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.following").value(false));
	}

	@Test
	void statusRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/follows/users/playertwo/status"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void statusReturnsCurrentFollowState() throws Exception {
		when(userRepository.findByUsername("playertwo")).thenReturn(Optional.of(user(OTHER_USER_ID, "playertwo")));
		when(followRepository.existsByFollowerIdAndFolloweeId(USER_ID, OTHER_USER_ID)).thenReturn(true);

		mockMvc.perform(get("/api/v1/follows/users/playertwo/status")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.following").value(true));
	}

	@Test
	void followersWorksWithoutAuthentication() throws Exception {
		User target = user(USER_ID, "playerone");
		User follower = user(OTHER_USER_ID, "playertwo");
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(target));
		when(followRepository.findByFolloweeIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(new Follow(follower, target))));

		mockMvc.perform(get("/api/v1/follows/users/playerone/followers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].username").value("playertwo"));
	}

	@Test
	void followingListWorksWithoutAuthentication() throws Exception {
		User target = user(USER_ID, "playerone");
		User followee = user(OTHER_USER_ID, "playertwo");
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(target));
		when(followRepository.findByFollowerIdOrderByCreatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(new Follow(target, followee))));

		mockMvc.perform(get("/api/v1/follows/users/playerone/following"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].username").value("playertwo"));
	}

	@Test
	void followersReturnsNotFoundForPrivateProfile() throws Exception {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(privateUser(USER_ID, "playerone")));

		mockMvc.perform(get("/api/v1/follows/users/playerone/followers"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Profile not found"));
	}

	private User user(UUID id, String username) {
		return privateOrPublicUser(id, username, ProfileVisibility.PUBLIC);
	}

	private User privateUser(UUID id, String username) {
		return privateOrPublicUser(id, username, ProfileVisibility.PRIVATE);
	}

	private User privateOrPublicUser(UUID id, String username, ProfileVisibility visibility) {
		User user = new User(username + "@example.com", username, "hash", Role.USER);
		user.setProfileVisibility(visibility);
		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}
}
