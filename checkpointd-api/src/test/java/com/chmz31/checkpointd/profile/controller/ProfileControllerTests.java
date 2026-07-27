package com.chmz31.checkpointd.profile.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import com.chmz31.checkpointd.game.entity.Game;
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
class ProfileControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID ENTRY_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

	@Test
	void publicProfileReturnsWithoutAuthentication() throws Exception {
		User user = user(ProfileVisibility.PUBLIC);
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user));
		when(libraryEntryRepository.countByUserId(USER_ID)).thenReturn(3L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.COMPLETED)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndRatingIsNotNull(USER_ID)).thenReturn(2L);
		when(libraryEntryRepository.averageRatingByUserId(USER_ID)).thenReturn(8.5);
		when(libraryEntryRepository.findTop8ByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(entry()));

		mockMvc.perform(get("/api/v1/profiles/playerone"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("playerone"))
				.andExpect(jsonPath("$.displayName").value("Player One"))
				.andExpect(jsonPath("$.bio").value("I play RPGs."))
				.andExpect(jsonPath("$.joinedAt").value("2026-01-01T00:00:00Z"))
				.andExpect(jsonPath("$.stats.totalGames").value(3))
				.andExpect(jsonPath("$.stats.completedGames").value(1))
				.andExpect(jsonPath("$.stats.ratedGames").value(2))
				.andExpect(jsonPath("$.stats.averageRating").value(8.5))
				.andExpect(jsonPath("$.recentGames", hasSize(1)))
				.andExpect(jsonPath("$.recentGames[0].libraryEntryId").value(ENTRY_ID.toString()))
				.andExpect(jsonPath("$.recentGames[0].gameTitle").value("Chrono Trigger"))
				.andExpect(jsonPath("$.recentGames[0].notes").doesNotExist());
	}

	@Test
	void missingPublicProfileReturnsNotFound() throws Exception {
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/profiles/missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Profile not found"));
	}

	@Test
	void privatePublicProfileReturnsNotFound() throws Exception {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PRIVATE)));

		mockMvc.perform(get("/api/v1/profiles/playerone"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Profile not found"));
	}

	@Test
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/profiles/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void updateMeRequiresAuthentication() throws Exception {
		mockMvc.perform(patch("/api/v1/profiles/me")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "displayName": "New Name"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturnsOwnProfile() throws Exception {
		User user = user(ProfileVisibility.PRIVATE);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(libraryEntryRepository.findTop8ByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/profiles/me")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("playerone"))
				.andExpect(jsonPath("$.profileVisibility").value("PRIVATE"));
	}

	@Test
	void updateMeUpdatesPublicProfileFieldsOnly() throws Exception {
		User user = user(ProfileVisibility.PUBLIC);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);
		when(libraryEntryRepository.findTop8ByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of());

		mockMvc.perform(patch("/api/v1/profiles/me")
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "displayName": "New Name",
								  "bio": "Now playing everything.",
								  "profileVisibility": "PRIVATE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("playerone"))
				.andExpect(jsonPath("$.displayName").value("New Name"))
				.andExpect(jsonPath("$.bio").value("Now playing everything."))
				.andExpect(jsonPath("$.profileVisibility").value("PRIVATE"));
	}

	private User user(ProfileVisibility visibility) {
		User user = new User("player@example.com", "playerone", "hash", Role.USER);
		user.setDisplayName("Player One");
		user.setBio("I play RPGs.");
		user.setProfileVisibility(visibility);
		ReflectionTestUtils.setField(user, "id", USER_ID);
		ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));

		return user;
	}

	private Game game() {
		Game game = new Game("Chrono Trigger");
		game.setSlug("chrono-trigger");
		game.setCoverUrl("https://img.example/cover.jpg");
		game.setGenres(List.of("RPG"));
		game.setPlatforms(List.of("SNES"));
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}

	private LibraryEntry entry() {
		LibraryEntry entry = new LibraryEntry(user(ProfileVisibility.PUBLIC), game(), LibraryStatus.COMPLETED);
		entry.setRating(9);
		entry.setNotes("Private note");
		ReflectionTestUtils.setField(entry, "id", ENTRY_ID);
		ReflectionTestUtils.setField(entry, "updatedAt", Instant.parse("2026-01-03T00:00:00Z"));

		return entry;
	}
}
