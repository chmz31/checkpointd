package com.chmz31.checkpointd.library.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.chmz31.checkpointd.game.entity.GameWebsite;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
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

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

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
								  "startedAt": "2026-01-01",
								  "completedAt": "2026-02-01"
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
				.andExpect(jsonPath("$.gameDevelopers[0]").value("Square"))
				.andExpect(jsonPath("$.gamePublishers[0]").value("Enix"))
				.andExpect(jsonPath("$.gameModes[0]").value("Single player"))
				.andExpect(jsonPath("$.gameThemes[0]").value("Time travel"))
				.andExpect(jsonPath("$.gamePlayerPerspectives[0]").value("Bird view"))
				.andExpect(jsonPath("$.gameWebsites[0].label").value("Official"))
				.andExpect(jsonPath("$.gameWebsites[0].url").value("https://www.square-enix-games.com/chrono-trigger"))
				.andExpect(jsonPath("$.gameWebsites[0].trusted").value(true))
				.andExpect(jsonPath("$.gameExternalRating").value(92.5))
				.andExpect(jsonPath("$.gameExternalRatingCount").value(50))
				.andExpect(jsonPath("$.gameScreenshotUrls[0]").value("https://img.example/shot.jpg"))
				.andExpect(jsonPath("$.gameArtworkUrls[0]").value("https://img.example/art.jpg"))
				.andExpect(jsonPath("$.gameBackdropUrl").value("https://img.example/art.jpg"))
				.andExpect(jsonPath("$.status").value("PLAYING"))
				.andExpect(jsonPath("$.rating").value(9))
				.andExpect(jsonPath("$.notes").value("Great"))
				.andExpect(jsonPath("$.startedAt").value("2026-01-01"))
				.andExpect(jsonPath("$.completedAt").value("2026-02-01"));
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
	void ratingBelowOneReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "gameId": "00000000-0000-0000-0000-000000000101",
								  "status": "PLAYING",
								  "rating": 0
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void listRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/library")
						.param("page", "0")
						.param("size", "24")
						.param("sort", "UPDATED_DESC"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listReturnsPaginatedCurrentUsersEntries() throws Exception {
		when(libraryEntryRepository.findUserLibrary(eq(USER_ID), isNull(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entry()), PageRequest.of(0, 24), 51));

		mockMvc.perform(get("/api/v1/library")
						.param("page", "0")
						.param("size", "24")
						.param("sort", "UPDATED_DESC")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$.content[0].gameTitle").value("Chrono Trigger"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(24))
				.andExpect(jsonPath("$.totalElements").value(51))
				.andExpect(jsonPath("$.totalPages").value(3))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(false));
	}

	@Test
	void listFiltersByStatus() throws Exception {
		when(libraryEntryRepository.findUserLibrary(eq(USER_ID), eq(LibraryStatus.PLAYING), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entry()), PageRequest.of(0, 24), 1));

		mockMvc.perform(get("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.param("status", "PLAYING"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].status").value("PLAYING"));
	}

	@Test
	void listAcceptsPaginationSearchAndSort() throws Exception {
		when(libraryEntryRepository.searchUserLibrary(eq(USER_ID), eq(LibraryStatus.PLAYING), eq("%devil%"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entry()), PageRequest.of(1, 12), 25));

		mockMvc.perform(get("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.param("page", "1")
						.param("size", "12")
						.param("status", "PLAYING")
						.param("q", "devil")
						.param("sort", "TITLE_ASC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(12))
				.andExpect(jsonPath("$.totalElements").value(25))
				.andExpect(jsonPath("$.totalPages").value(3));
	}

	@Test
	void listTreatsBlankSearchLikeNoSearch() throws Exception {
		when(libraryEntryRepository.findUserLibrary(eq(USER_ID), isNull(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entry()), PageRequest.of(0, 24), 1));

		mockMvc.perform(get("/api/v1/library")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.param("page", "0")
						.param("size", "24")
						.param("q", "   ")
						.param("sort", "UPDATED_DESC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)));
	}

	@Test
	void getByGameReturnsCurrentUsersEntry() throws Exception {
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(entry()));

		mockMvc.perform(get("/api/v1/library/by-game/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$.gameId").value(GAME_ID.toString()))
				.andExpect(jsonPath("$.gameTitle").value("Chrono Trigger"));
	}

	@Test
	void getByGameReturnsNotFoundWhenGameIsNotInCurrentUsersLibrary() throws Exception {
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/library/by-game/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Library entry not found"));
	}

	@Test
	void getByGameDoesNotReturnAnotherUsersEntry() throws Exception {
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/library/by-game/{gameId}", GAME_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Library entry not found"));
	}

	@Test
	void getByExternalGameReturnsCurrentUsersEntry() throws Exception {
		Game game = game();

		when(gameRepository.findByExternalProviderAndExternalId("igdb", "123")).thenReturn(Optional.of(game));
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(entry()));

		mockMvc.perform(get("/api/v1/library/by-external-game")
						.param("provider", "IGDB")
						.param("externalId", "123")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$.gameId").value(GAME_ID.toString()))
				.andExpect(jsonPath("$.gameTitle").value("Chrono Trigger"));
	}

	@Test
	void getByExternalGameReturnsNotFoundWhenGameIsNotCached() throws Exception {
		when(gameRepository.findByExternalProviderAndExternalId("igdb", "123")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/library/by-external-game")
						.param("provider", "igdb")
						.param("externalId", "123")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Library entry not found"));
	}

	@Test
	void getByExternalGameReturnsNotFoundWhenGameIsNotInCurrentUsersLibrary() throws Exception {
		Game game = game();

		when(gameRepository.findByExternalProviderAndExternalId("igdb", "123")).thenReturn(Optional.of(game));
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/library/by-external-game")
						.param("provider", "igdb")
						.param("externalId", "123")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Library entry not found"));
	}

	@Test
	void getByExternalGameDoesNotReturnAnotherUsersEntry() throws Exception {
		Game game = game();

		when(gameRepository.findByExternalProviderAndExternalId("igdb", "123")).thenReturn(Optional.of(game));
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/library/by-external-game")
						.param("provider", "igdb")
						.param("externalId", "123")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Library entry not found"));
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
								  "startedAt": "2026-01-01",
								  "completedAt": "2026-02-01"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.rating").value(10))
				.andExpect(jsonPath("$.startedAt").value("2026-01-01"))
				.andExpect(jsonPath("$.completedAt").value("2026-02-01"));
	}

	@Test
	void updateClearsNullableTrackingFields() throws Exception {
		LibraryEntry entry = entry();
		entry.setRating(9);
		entry.setNotes("Great");
		entry.setStartedAt(LocalDate.parse("2026-01-01"));
		entry.setCompletedAt(LocalDate.parse("2026-02-01"));

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));
		when(libraryEntryRepository.save(entry)).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(patch("/api/v1/library/{entryId}", ENTRY_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "status": "PLAYING",
								  "rating": null,
								  "notes": null,
								  "startedAt": null,
								  "completedAt": null
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PLAYING"))
				.andExpect(jsonPath("$.rating").doesNotExist())
				.andExpect(jsonPath("$.notes").doesNotExist())
				.andExpect(jsonPath("$.startedAt").doesNotExist())
				.andExpect(jsonPath("$.completedAt").doesNotExist());
	}

	@Test
	void updateRatingAboveTenReturnsBadRequest() throws Exception {
		mockMvc.perform(patch("/api/v1/library/{entryId}", ENTRY_ID)
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
	void syncMetadataReturnsUpdatedEntry() throws Exception {
		LibraryEntry entry = entry();
		Game game = entry.getGame();
		game.setExternalProvider("igdb");
		game.setExternalId("123");

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));
		when(externalGameImportService.syncMetadata(game)).thenAnswer(invocation -> {
			game.setTitle("Chrono Trigger Updated");
			game.setSummary("Updated summary");
			game.setGenres(List.of("RPG", "Adventure"));
			game.setPlatforms(List.of("PC"));
			game.setDevelopers(List.of("Square"));
			game.setPublishers(List.of("Enix"));
			game.setGameModes(List.of("Single player"));
			game.setThemes(List.of("Adventure"));
			game.setPlayerPerspectives(List.of("Side view"));
			game.setWebsites(List.of(new GameWebsite(
					"Official", "https://www.square-enix-games.com/chrono-trigger", true)));
			game.setExternalRating(94.0);
			game.setExternalRatingCount(75);
			game.setScreenshotUrls(List.of("https://img.example/new-shot.jpg"));
			game.setArtworkUrls(List.of("https://img.example/new-art.jpg"));
			game.setBackdropUrl("https://img.example/new-art.jpg");
			return game;
		});

		mockMvc.perform(post("/api/v1/library/{entryId}/sync-metadata", ENTRY_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameTitle").value("Chrono Trigger Updated"))
				.andExpect(jsonPath("$.gameSummary").value("Updated summary"))
				.andExpect(jsonPath("$.gameGenres[0]").value("RPG"))
				.andExpect(jsonPath("$.gamePlatforms[0]").value("PC"))
				.andExpect(jsonPath("$.gameDevelopers[0]").value("Square"))
				.andExpect(jsonPath("$.gamePublishers[0]").value("Enix"))
				.andExpect(jsonPath("$.gameModes[0]").value("Single player"))
				.andExpect(jsonPath("$.gameThemes[0]").value("Adventure"))
				.andExpect(jsonPath("$.gamePlayerPerspectives[0]").value("Side view"))
				.andExpect(jsonPath("$.gameWebsites[0].label").value("Official"))
				.andExpect(jsonPath("$.gameWebsites[0].url").value("https://www.square-enix-games.com/chrono-trigger"))
				.andExpect(jsonPath("$.gameWebsites[0].trusted").value(true))
				.andExpect(jsonPath("$.gameExternalRating").value(94.0))
				.andExpect(jsonPath("$.gameExternalRatingCount").value(75))
				.andExpect(jsonPath("$.gameScreenshotUrls[0]").value("https://img.example/new-shot.jpg"))
				.andExpect(jsonPath("$.gameArtworkUrls[0]").value("https://img.example/new-art.jpg"))
				.andExpect(jsonPath("$.gameBackdropUrl").value("https://img.example/new-art.jpg"))
				.andExpect(jsonPath("$.gameMetadataSyncAvailable").value(true));
	}

	@Test
	void responseHandlesMissingMediaSafely() throws Exception {
		Game game = new Game("Local Game");
		ReflectionTestUtils.setField(game, "id", GAME_ID);
		LibraryEntry entry = withEntryMetadata(new LibraryEntry(user(), game, LibraryStatus.BACKLOG));

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));

		mockMvc.perform(get("/api/v1/library/{entryId}", ENTRY_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gameScreenshotUrls", hasSize(0)))
				.andExpect(jsonPath("$.gameArtworkUrls", hasSize(0)))
				.andExpect(jsonPath("$.gameBackdropUrl").doesNotExist());
	}

	@Test
	void syncMetadataMissingEntryReturnsNotFound() throws Exception {
		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/v1/library/{entryId}/sync-metadata", ENTRY_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Library entry not found"));
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
		game.setExternalProvider("igdb");
		game.setExternalId("123");
		game.setSlug("chrono-trigger");
		game.setCoverUrl("https://img.example/cover.jpg");
		game.setSummary("Time travel RPG");
		game.setGenres(List.of("RPG"));
		game.setPlatforms(List.of("SNES"));
		game.setDevelopers(List.of("Square"));
		game.setPublishers(List.of("Enix"));
		game.setGameModes(List.of("Single player"));
		game.setThemes(List.of("Time travel"));
		game.setPlayerPerspectives(List.of("Bird view"));
		game.setWebsites(List.of(new GameWebsite(
				"Official", "https://www.square-enix-games.com/chrono-trigger", true)));
		game.setExternalRating(92.5);
		game.setExternalRatingCount(50);
		game.setScreenshotUrls(List.of("https://img.example/shot.jpg"));
		game.setArtworkUrls(List.of("https://img.example/art.jpg"));
		game.setBackdropUrl("https://img.example/art.jpg");
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
