package com.chmz31.checkpointd.externalgames.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import com.chmz31.checkpointd.externalgames.dto.ImportExternalGameRequest;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.externalgames.service.ImportedGameResult;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.LocalDate;
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
class ExternalGameImportControllerTests {

	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@Test
	void authenticatedImportCreatedReturnsCreated() throws Exception {
		when(externalGameImportService.importGame(new ImportExternalGameRequest("igdb", "112964")))
				.thenReturn(new ImportedGameResult(game(), true));

		mockMvc.perform(post("/api/v1/external-games/import")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "provider": "igdb",
								  "externalId": "112964"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(GAME_ID.toString()))
				.andExpect(jsonPath("$.externalProvider").value("igdb"))
				.andExpect(jsonPath("$.externalId").value("112964"))
				.andExpect(jsonPath("$.title").value("Hades"));
	}

	@Test
	void authenticatedImportExistingReturnsOk() throws Exception {
		when(externalGameImportService.importGame(new ImportExternalGameRequest("igdb", "112964")))
				.thenReturn(new ImportedGameResult(game(), false));

		mockMvc.perform(post("/api/v1/external-games/import")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "provider": "igdb",
								  "externalId": "112964"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Hades"));
	}

	@Test
	void unauthenticatedImportReturnsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/external-games/import")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "provider": "igdb",
								  "externalId": "112964"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void validationErrorReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/external-games/import")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "provider": "",
								  "externalId": ""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void unsupportedProviderReturnsBadRequest() throws Exception {
		when(externalGameImportService.importGame(new ImportExternalGameRequest("steam", "112964")))
				.thenThrow(new BadRequestException("Unsupported external game provider"));

		mockMvc.perform(post("/api/v1/external-games/import")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "provider": "steam",
								  "externalId": "112964"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unsupported external game provider"));
	}

	@Test
	void missingExternalGameReturnsNotFound() throws Exception {
		when(externalGameImportService.importGame(new ImportExternalGameRequest("igdb", "112964")))
				.thenThrow(new ResourceNotFoundException("External game not found"));

		mockMvc.perform(post("/api/v1/external-games/import")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "provider": "igdb",
								  "externalId": "112964"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("External game not found"));
	}

	@Test
	void upstreamErrorReturnsServiceUnavailable() throws Exception {
		when(externalGameImportService.importGame(new ImportExternalGameRequest("igdb", "112964")))
				.thenThrow(new ServiceUnavailableException("External game provider is unavailable"));

		mockMvc.perform(post("/api/v1/external-games/import")
						.with(jwt())
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "provider": "igdb",
								  "externalId": "112964"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value("External game provider is unavailable"));
	}

	private Game game() {
		Game game = new Game("Hades");
		game.setExternalProvider("igdb");
		game.setExternalId("112964");
		game.setSlug("hades");
		game.setCoverUrl("https://images.example/hades.jpg");
		game.setReleaseDate(LocalDate.of(2020, 9, 17));
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}
}
