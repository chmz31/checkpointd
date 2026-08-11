package com.chmz31.checkpointd.list.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.entity.GameListItem;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameListControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameListRepository gameListRepository;

	@MockitoBean
	private GameListItemRepository gameListItemRepository;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private FollowRepository followRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private ReviewRepository reviewRepository;

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

	@MockitoBean
	private ListLikeRepository listLikeRepository;

	@MockitoBean
	private ReviewLikeRepository reviewLikeRepository;

	@MockitoBean
	private ListCommentRepository listCommentRepository;

	@MockitoBean
	private ReviewCommentRepository reviewCommentRepository;

	@MockitoBean
	private ListCommentReportRepository listCommentReportRepository;

	@MockitoBean
	private ReviewCommentReportRepository reviewCommentReportRepository;

	@MockitoBean
	private ListCommentLikeRepository listCommentLikeRepository;

	@MockitoBean
	private ReviewCommentLikeRepository reviewCommentLikeRepository;

	@Test
	void createRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/lists")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "name": "Favorites"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanCreateList() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameListRepository.save(any(GameList.class))).thenAnswer(invocation -> withMetadata(invocation.getArgument(0)));

		mockMvc.perform(post("/api/v1/lists")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "name": "Cozy games",
								  "description": "Comfort picks.",
								  "visibility": "PRIVATE"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(LIST_ID.toString()))
				.andExpect(jsonPath("$.name").value("Cozy games"))
				.andExpect(jsonPath("$.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.owner").value(true))
				.andExpect(jsonPath("$.itemCount").value(0))
				.andExpect(jsonPath("$.likeCount").value(0))
				.andExpect(jsonPath("$.liked").value(false));
	}

	@Test
	void createRejectsBlankName() throws Exception {
		mockMvc.perform(post("/api/v1/lists")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "name": "   "
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void myListsRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/lists/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void myListsReturnsOwnersLists() throws Exception {
		when(gameListRepository.findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(list(ListVisibility.PRIVATE)), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/v1/lists/me")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.content[0].owner").value(true));
	}

	@Test
	void myListReturnsDetailWithItems() throws Exception {
		GameList list = list(ListVisibility.PRIVATE);
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of(item(list)));

		mockMvc.perform(get("/api/v1/lists/me/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].gameTitle").value("Chrono Trigger"));
	}

	@Test
	void updateReturnsUpdatedList() throws Exception {
		GameList list = list(ListVisibility.PUBLIC);
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameListRepository.save(list)).thenReturn(list);

		mockMvc.perform(patch("/api/v1/lists/me/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "name": "Renamed",
								  "visibility": "PRIVATE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed"))
				.andExpect(jsonPath("$.visibility").value("PRIVATE"));
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));

		mockMvc.perform(delete("/api/v1/lists/me/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void addItemAddsGameToList() throws Exception {
		GameList list = list(ListVisibility.PUBLIC);
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(gameListItemRepository.existsByListIdAndGameId(LIST_ID, GAME_ID)).thenReturn(false);
		when(gameListItemRepository.maxPositionByListId(LIST_ID)).thenReturn(0);
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of(item(list)));

		mockMvc.perform(post("/api/v1/lists/me/{listId}/items", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "gameId": "00000000-0000-0000-0000-000000000101"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)));
	}

	@Test
	void removeItemReturnsUpdatedList() throws Exception {
		GameList list = list(ListVisibility.PUBLIC);
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameListItemRepository.findByListIdAndGameId(LIST_ID, GAME_ID)).thenReturn(Optional.of(item(list)));
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of());

		mockMvc.perform(delete("/api/v1/lists/me/{listId}/items/{gameId}", LIST_ID, GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void userListsWorksWithoutAuthentication() throws Exception {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameListRepository.findByUserUsernameAndUserProfileVisibilityAndVisibilityOrderByUpdatedAtDesc(
				eq("playerone"), eq(ProfileVisibility.PUBLIC), eq(ListVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(list(ListVisibility.PUBLIC)), PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/v1/lists/users/playerone"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].owner").value(false));
	}

	@Test
	void userListReturnsPublicListDetail() throws Exception {
		when(gameListRepository.findByIdAndUserUsernameAndVisibilityAndUserProfileVisibility(
				LIST_ID, "playerone", ListVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of());
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(4L);

		mockMvc.perform(get("/api/v1/lists/users/playerone/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.owner").value(false))
				.andExpect(jsonPath("$.likeCount").value(4))
				.andExpect(jsonPath("$.liked").value(false));
	}

	@Test
	void userListReflectsLikedStateForCurrentUser() throws Exception {
		when(gameListRepository.findByIdAndUserUsernameAndVisibilityAndUserProfileVisibility(
				LIST_ID, "playerone", ListVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of());
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(1L);
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(true);

		mockMvc.perform(get("/api/v1/lists/users/playerone/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(true));
	}

	@Test
	void userListReturnsNotFoundForPrivateList() throws Exception {
		when(gameListRepository.findByIdAndUserUsernameAndVisibilityAndUserProfileVisibility(
				LIST_ID, "playerone", ListVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/lists/users/playerone/{listId}", LIST_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("List not found"));
	}

	private User user(ProfileVisibility profileVisibility) {
		User user = new User("player@example.com", "playerone", "hash", Role.USER);
		user.setDisplayName("Player One");
		user.setProfileVisibility(profileVisibility);
		ReflectionTestUtils.setField(user, "id", USER_ID);

		return user;
	}

	private Game game() {
		Game game = new Game("Chrono Trigger");
		game.setSlug("chrono-trigger");
		game.setCoverUrl("https://img.example/cover.jpg");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}

	private GameList list(ListVisibility visibility) {
		GameList list = new GameList(user(ProfileVisibility.PUBLIC), "Favorites");
		list.setVisibility(visibility);
		return withMetadata(list);
	}

	private GameList withMetadata(GameList list) {
		ReflectionTestUtils.setField(list, "id", LIST_ID);
		ReflectionTestUtils.setField(list, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(list, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));

		return list;
	}

	private GameListItem item(GameList list) {
		GameListItem item = new GameListItem(list, game(), 1);
		ReflectionTestUtils.setField(item, "id", UUID.fromString("00000000-0000-0000-0000-000000000501"));
		ReflectionTestUtils.setField(item, "createdAt", Instant.parse("2026-01-03T00:00:00Z"));

		return item;
	}
}
