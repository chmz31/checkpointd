package com.chmz31.checkpointd.library.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.entity.User;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID ENTRY_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	void createRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/library")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "gameId": "00000000-0000-0000-0000-000000000101",
								  "status": "PLAYING"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createReturnsCreatedEntry() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(libraryEntryRepository.existsByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(false);
		when(libraryEntryRepository.save(any(LibraryEntry.class))).thenAnswer(invocation -> withEntryMetadata(invocation.getArgument(0)));

		mockMvc.perform(post("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "gameId": "00000000-0000-0000-0000-000000000101",
								  "status": "PLAYING",
								  "rating": 9,
								  "notes": "Great",
								  "startedAt": "2026-01-01T00:00:00Z"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$.gameId").value(GAME_ID.toString()))
				.andExpect(jsonPath("$.gameTitle").value("Chrono Trigger"))
				.andExpect(jsonPath("$.gameSlug").value("chrono-trigger"))
				.andExpect(jsonPath("$.gameCoverUrl").value("https://img.example/cover.jpg"))
				.andExpect(jsonPath("$.gameSummary").value("Time travel RPG"))
				.andExpect(jsonPath("$.gameGenres[0]").value("RPG"))
				.andExpect(jsonPath("$.gamePlatforms[0]").value("SNES"))
				.andExpect(jsonPath("$.status").value("PLAYING"))
				.andExpect(jsonPath("$.rating").value(9))
				.andExpect(jsonPath("$.notes").value("Great"));
	}

	@Test
	void duplicateReturnsConflict() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(libraryEntryRepository.existsByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(true);

		mockMvc.perform(post("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "gameId": "00000000-0000-0000-0000-000000000101",
								  "status": "PLAYING"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Game is already in library"));
	}

	@Test
	void missingEntryReturnsNotFound() throws Exception {
		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/library/{entryId}", ENTRY_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Library entry not found"));
	}

	@Test
	void validationErrorReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "status": "PLAYING",
								  "rating": 11
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void listReturnsCurrentUsersEntries() throws Exception {
		when(libraryEntryRepository.findTop50ByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(entry()));

		mockMvc.perform(get("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$[0].gameTitle").value("Chrono Trigger"));
	}

	@Test
	void listFiltersByStatus() throws Exception {
		when(libraryEntryRepository.findTop50ByUserIdAndStatusOrderByUpdatedAtDesc(USER_ID, LibraryStatus.PLAYING))
				.thenReturn(List.of(entry()));

		mockMvc.perform(get("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.param("status", "PLAYING"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status").value("PLAYING"));
	}

	@Test
	void statsRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/library/stats"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void statsReturnsCurrentUsersCounts() throws Exception {
		when(libraryEntryRepository.countByUserId(USER_ID)).thenReturn(4L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.WISHLIST)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.BACKLOG)).thenReturn(0L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.PLAYING)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.COMPLETED)).thenReturn(2L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.DROPPED)).thenReturn(0L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.PAUSED)).thenReturn(0L);
		when(libraryEntryRepository.countByUserIdAndRatingIsNotNull(USER_ID)).thenReturn(2L);
		when(libraryEntryRepository.averageRatingByUserId(USER_ID)).thenReturn(8.5);

		mockMvc.perform(get("/api/v1/library/stats")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalEntries").value(4))
				.andExpect(jsonPath("$.wishlistCount").value(1))
				.andExpect(jsonPath("$.backlogCount").value(0))
				.andExpect(jsonPath("$.playingCount").value(1))
				.andExpect(jsonPath("$.completedCount").value(2))
				.andExpect(jsonPath("$.droppedCount").value(0))
				.andExpect(jsonPath("$.pausedCount").value(0))
				.andExpect(jsonPath("$.ratedCount").value(2))
				.andExpect(jsonPath("$.averageRating").value(8.5));
	}

	@Test
	void updateReturnsUpdatedEntry() throws Exception {
		LibraryEntry entry = entry();

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));
		when(libraryEntryRepository.save(entry)).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(patch("/api/v1/library/{entryId}", ENTRY_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "status": "COMPLETED",
								  "rating": 10,
								  "completedAt": "2026-02-01T00:00:00Z"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.rating").value(10))
				.andExpect(jsonPath("$.completedAt").value("2026-02-01T00:00:00Z"));
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry()));

		mockMvc.perform(delete("/api/v1/library/{entryId}", ENTRY_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	private User user() {
		User user = new User("player@example.com", "playerone", "hash", Role.USER);
		ReflectionTestUtils.setField(user, "id", USER_ID);

		return user;
	}

	private Game game() {
		Game game = new Game("Chrono Trigger");
		game.setSlug("chrono-trigger");
		game.setCoverUrl("https://img.example/cover.jpg");
		game.setSummary("Time travel RPG");
		game.setGenres(List.of("RPG"));
		game.setPlatforms(List.of("SNES"));
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}

	private LibraryEntry entry() {
		return withEntryMetadata(new LibraryEntry(user(), game(), LibraryStatus.PLAYING));
	}

	private LibraryEntry withEntryMetadata(LibraryEntry entry) {
		ReflectionTestUtils.setField(entry, "id", ENTRY_ID);
		ReflectionTestUtils.setField(entry, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(entry, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));

		return entry;
	}
}
