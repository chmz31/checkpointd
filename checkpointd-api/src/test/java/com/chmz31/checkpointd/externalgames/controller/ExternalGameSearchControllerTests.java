package com.chmz31.checkpointd.externalgames.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import com.chmz31.checkpointd.externalgames.service.ExternalGameSearchService;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExternalGameSearchControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ExternalGameSearchService externalGameSearchService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@Test
	void authenticatedSearchReturnsResults() throws Exception {
		when(externalGameSearchService.search("chrono")).thenReturn(List.of(new ExternalGameSearchResult(
				"igdb", "123", "Chrono Trigger", "chrono-trigger", "https://images.example/cover.jpg",
				LocalDate.of(1995, 3, 11), "Time travel RPG", List.of("RPG"), List.of("SNES"))));

		mockMvc.perform(get("/api/v1/external-games/search")
						.with(jwt())
						.param("q", "chrono"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].provider").value("igdb"))
				.andExpect(jsonPath("$[0].externalId").value("123"))
				.andExpect(jsonPath("$[0].title").value("Chrono Trigger"))
				.andExpect(jsonPath("$[0].slug").value("chrono-trigger"))
				.andExpect(jsonPath("$[0].coverUrl").value("https://images.example/cover.jpg"))
				.andExpect(jsonPath("$[0].releaseDate").value("1995-03-11"))
				.andExpect(jsonPath("$[0].summary").value("Time travel RPG"))
				.andExpect(jsonPath("$[0].genres[0]").value("RPG"))
				.andExpect(jsonPath("$[0].platforms[0]").value("SNES"));
	}

	@Test
	void unauthenticatedSearchReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/external-games/search")
						.param("q", "chrono"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void blankQueryReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/external-games/search")
						.with(jwt())
						.param("q", " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void missingQueryReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/external-games/search")
						.with(jwt()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void upstreamErrorReturnsServiceUnavailable() throws Exception {
		when(externalGameSearchService.search("chrono"))
				.thenThrow(new ServiceUnavailableException("External game provider is unavailable"));

		mockMvc.perform(get("/api/v1/external-games/search")
						.with(jwt())
						.param("q", "chrono"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.message").value("External game provider is unavailable"));
	}
}
