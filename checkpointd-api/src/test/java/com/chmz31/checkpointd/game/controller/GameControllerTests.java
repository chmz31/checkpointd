package com.chmz31.checkpointd.game.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.LocalDate;
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
class GameControllerTests {

	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@Test
	void createRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/games")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "title": "Chrono Trigger"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createReturnsCreatedGame() throws Exception {
		when(gameRepository.existsByExternalProviderAndExternalId("igdb", "123")).thenReturn(false);
		when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

		mockMvc.perform(post("/api/v1/games")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "externalProvider": "IGDB",
								  "externalId": " 123 ",
								  "title": "Chrono Trigger",
								  "slug": "chrono-trigger",
								  "coverUrl": "https://img.example/cover.jpg",
								  "releaseDate": "1995-03-11"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(GAME_ID.toString()))
				.andExpect(jsonPath("$.externalProvider").value("igdb"))
				.andExpect(jsonPath("$.externalId").value("123"))
				.andExpect(jsonPath("$.title").value("Chrono Trigger"))
				.andExpect(jsonPath("$.slug").value("chrono-trigger"))
				.andExpect(jsonPath("$.coverUrl").value("https://img.example/cover.jpg"))
				.andExpect(jsonPath("$.releaseDate").value("1995-03-11"));
	}

	@Test
	void duplicateExternalIdentityReturnsConflict() throws Exception {
		when(gameRepository.existsByExternalProviderAndExternalId("igdb", "123")).thenReturn(true);

		mockMvc.perform(post("/api/v1/games")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "externalProvider": "igdb",
								  "externalId": "123",
								  "title": "Chrono Trigger"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Game external identity already exists"));
	}

	@Test
	void invalidExternalIdentityReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/games")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "externalProvider": "igdb",
								  "title": "Chrono Trigger"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("externalProvider and externalId must be provided together"));
	}

	@Test
	void validationErrorReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/games")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "title": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void listReturnsDefaultGames() throws Exception {
		when(gameRepository.findTop20ByOrderByTitleAsc()).thenReturn(List.of(withId(new Game("Chrono Trigger"))));

		mockMvc.perform(get("/api/v1/games")
						.with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(GAME_ID.toString()))
				.andExpect(jsonPath("$[0].title").value("Chrono Trigger"));
	}

	@Test
	void listSearchesGamesByTitle() throws Exception {
		when(gameRepository.findTop20ByTitleContainingIgnoreCaseOrderByTitleAsc("chrono"))
				.thenReturn(List.of(withId(new Game("Chrono Trigger"))));

		mockMvc.perform(get("/api/v1/games")
						.with(jwt())
						.param("q", "chrono"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("Chrono Trigger"));
	}

	@Test
	void getReturnsGame() throws Exception {
		Game game = withId(new Game("Chrono Trigger"));
		game.setReleaseDate(LocalDate.of(1995, 3, 11));

		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

		mockMvc.perform(get("/api/v1/games/{gameId}", GAME_ID)
						.with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(GAME_ID.toString()))
				.andExpect(jsonPath("$.title").value("Chrono Trigger"))
				.andExpect(jsonPath("$.releaseDate").value("1995-03-11"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void missingGameReturnsNotFound() throws Exception {
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/games/{gameId}", GAME_ID)
						.with(jwt()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Game not found"));
	}

	private Game withId(Game game) {
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}
}
