package com.chmz31.checkpointd.externalgames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameWebsite;
import com.chmz31.checkpointd.externalgames.dto.ImportExternalGameRequest;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.model.MetadataSyncStatus;
import com.chmz31.checkpointd.game.repository.GameRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExternalGameImportServiceTests {

	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Mock
	private GameRepository gameRepository;

	@Mock
	private IgdbClient igdbClient;

	@InjectMocks
	private ExternalGameImportService externalGameImportService;

	@Test
	void importReturnsExistingLocalGameWithoutCallingIgdb() {
		Game existing = game();
		when(gameRepository.findByExternalProviderAndExternalId("igdb", "112964")).thenReturn(Optional.of(existing));

		ImportedGameResult result = externalGameImportService.importGame(
				new ImportExternalGameRequest("IGDB", " 112964 "));

		assertThat(result.game()).isSameAs(existing);
		assertThat(result.created()).isFalse();
		verify(igdbClient, never()).fetchById("112964");
		verify(gameRepository, never()).saveAndFlush(any(Game.class));
	}

	@Test
	void importFetchesAndSavesNewIgdbGame() {
		when(gameRepository.findByExternalProviderAndExternalId("igdb", "112964")).thenReturn(Optional.empty());
		when(igdbClient.fetchById("112964")).thenReturn(new ExternalGameSearchResult(
				"igdb", "112964", "Hades", "hades", "https://images.example/hades.jpg",
				LocalDate.of(2020, 9, 17), "Escape the underworld", List.of("Roguelike", "Action"),
				List.of("PC", "Switch"), List.of("Supergiant Games"), List.of("Private Division"),
				List.of("Single player"), List.of("Action"), List.of("Bird view"),
				List.of(new ExternalGameWebsite("Official", "https://www.supergiantgames.com/games/hades/", true)),
				93.5, 212,
				List.of("https://images.example/screenshot.jpg"),
				List.of("https://images.example/artwork.jpg"), "https://images.example/artwork.jpg"));
		when(gameRepository.saveAndFlush(any(Game.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

		ImportedGameResult result = externalGameImportService.importGame(
				new ImportExternalGameRequest("igdb", "112964"));

		ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
		verify(gameRepository).saveAndFlush(gameCaptor.capture());
		Game saved = gameCaptor.getValue();

		assertThat(result.created()).isTrue();
		assertThat(result.game()).isSameAs(saved);
		assertThat(saved.getExternalProvider()).isEqualTo("igdb");
		assertThat(saved.getExternalId()).isEqualTo("112964");
		assertThat(saved.getTitle()).isEqualTo("Hades");
		assertThat(saved.getSlug()).isEqualTo("hades");
		assertThat(saved.getCoverUrl()).isEqualTo("https://images.example/hades.jpg");
		assertThat(saved.getReleaseDate()).isEqualTo(LocalDate.of(2020, 9, 17));
		assertThat(saved.getSummary()).isEqualTo("Escape the underworld");
		assertThat(saved.getGenres()).containsExactly("Roguelike", "Action");
		assertThat(saved.getPlatforms()).containsExactly("PC", "Switch");
		assertThat(saved.getDevelopers()).containsExactly("Supergiant Games");
		assertThat(saved.getPublishers()).containsExactly("Private Division");
		assertThat(saved.getGameModes()).containsExactly("Single player");
		assertThat(saved.getThemes()).containsExactly("Action");
		assertThat(saved.getPlayerPerspectives()).containsExactly("Bird view");
		assertThat(saved.getWebsites()).extracting("label", "url", "trusted")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(
						"Official", "https://www.supergiantgames.com/games/hades/", true));
		assertThat(saved.getExternalRating()).isEqualTo(93.5);
		assertThat(saved.getExternalRatingCount()).isEqualTo(212);
		assertThat(saved.getMetadataSyncedAt()).isNotNull();
		assertThat(saved.getMetadataSyncAttemptedAt()).isNotNull();
		assertThat(saved.getMetadataSyncStatus()).isEqualTo(MetadataSyncStatus.SUCCESS);
		assertThat(saved.getMetadataSyncError()).isNull();
		assertThat(saved.getScreenshotUrls()).containsExactly("https://images.example/screenshot.jpg");
		assertThat(saved.getArtworkUrls()).containsExactly("https://images.example/artwork.jpg");
		assertThat(saved.getBackdropUrl()).isEqualTo("https://images.example/artwork.jpg");
	}

	@Test
	void importRejectsUnsupportedProvider() {
		assertThatThrownBy(() -> externalGameImportService.importGame(
				new ImportExternalGameRequest("steam", "112964")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Unsupported external game provider");
	}

	@Test
	void importRejectsNonNumericIgdbId() {
		assertThatThrownBy(() -> externalGameImportService.importGame(
				new ImportExternalGameRequest("igdb", "abc")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("IGDB externalId must be numeric");
	}

	@Test
	void importRejectsMissingExternalGame() {
		when(gameRepository.findByExternalProviderAndExternalId("igdb", "112964")).thenReturn(Optional.empty());
		when(igdbClient.fetchById("112964")).thenReturn(null);

		assertThatThrownBy(() -> externalGameImportService.importGame(
				new ImportExternalGameRequest("igdb", "112964")))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("External game not found");
	}

	@Test
	void importPropagatesUpstreamError() {
		when(gameRepository.findByExternalProviderAndExternalId("igdb", "112964")).thenReturn(Optional.empty());
		when(igdbClient.fetchById("112964"))
				.thenThrow(new ServiceUnavailableException("External game provider is unavailable"));

		assertThatThrownBy(() -> externalGameImportService.importGame(
				new ImportExternalGameRequest("igdb", "112964")))
				.isInstanceOf(ServiceUnavailableException.class)
				.hasMessage("External game provider is unavailable");
	}

	@Test
	void importReturnsExistingGameAfterUniqueConstraintRace() {
		Game existing = game();
		when(gameRepository.findByExternalProviderAndExternalId("igdb", "112964"))
				.thenReturn(Optional.empty(), Optional.of(existing));
		when(igdbClient.fetchById("112964")).thenReturn(new ExternalGameSearchResult(
				"igdb", "112964", "Hades", "hades", "https://images.example/hades.jpg",
				LocalDate.of(2020, 9, 17)));
		when(gameRepository.saveAndFlush(any(Game.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		ImportedGameResult result = externalGameImportService.importGame(
				new ImportExternalGameRequest("igdb", "112964"));

		assertThat(result.game()).isSameAs(existing);
		assertThat(result.created()).isFalse();
	}

	@Test
	void syncMetadataUpdatesExistingGame() {
		Game game = game();
		when(igdbClient.fetchById("112964")).thenReturn(new ExternalGameSearchResult(
				"igdb", "112964", "Hades II", "hades-ii", "https://images.example/hades-ii.jpg",
				LocalDate.of(2024, 5, 6), "Return to the underworld", List.of("Action", "Roguelike"),
				List.of("PC"), List.of("Supergiant Games"), List.of("Supergiant Games"),
				List.of("Single player"), List.of("Fantasy"), List.of("Bird view"),
				List.of(new ExternalGameWebsite("Official", "https://www.supergiantgames.com/games/hades-ii/", true)),
				91.0, 54,
				List.of("https://images.example/new-shot.jpg"),
				List.of("https://images.example/new-art.jpg"), "https://images.example/new-art.jpg"));
		when(gameRepository.save(game)).thenReturn(game);

		Game synced = externalGameImportService.syncMetadata(game);

		assertThat(synced).isSameAs(game);
		assertThat(game.getTitle()).isEqualTo("Hades II");
		assertThat(game.getSlug()).isEqualTo("hades-ii");
		assertThat(game.getCoverUrl()).isEqualTo("https://images.example/hades-ii.jpg");
		assertThat(game.getReleaseDate()).isEqualTo(LocalDate.of(2024, 5, 6));
		assertThat(game.getSummary()).isEqualTo("Return to the underworld");
		assertThat(game.getGenres()).containsExactly("Action", "Roguelike");
		assertThat(game.getPlatforms()).containsExactly("PC");
		assertThat(game.getDevelopers()).containsExactly("Supergiant Games");
		assertThat(game.getPublishers()).containsExactly("Supergiant Games");
		assertThat(game.getGameModes()).containsExactly("Single player");
		assertThat(game.getThemes()).containsExactly("Fantasy");
		assertThat(game.getPlayerPerspectives()).containsExactly("Bird view");
		assertThat(game.getWebsites()).extracting("label", "url", "trusted")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(
						"Official", "https://www.supergiantgames.com/games/hades-ii/", true));
		assertThat(game.getExternalRating()).isEqualTo(91.0);
		assertThat(game.getExternalRatingCount()).isEqualTo(54);
		assertThat(game.getMetadataSyncedAt()).isNotNull();
		assertThat(game.getMetadataSyncAttemptedAt()).isNotNull();
		assertThat(game.getMetadataSyncStatus()).isEqualTo(MetadataSyncStatus.SUCCESS);
		assertThat(game.getMetadataSyncError()).isNull();
		assertThat(game.getScreenshotUrls()).containsExactly("https://images.example/new-shot.jpg");
		assertThat(game.getArtworkUrls()).containsExactly("https://images.example/new-art.jpg");
		assertThat(game.getBackdropUrl()).isEqualTo("https://images.example/new-art.jpg");
	}

	@Test
	void syncMetadataRejectsGameWithoutExternalIdentity() {
		Game game = new Game("Local Game");

		assertThatThrownBy(() -> externalGameImportService.syncMetadata(game))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Game cannot be synced without external identity");
	}

	@Test
	void syncMetadataRejectsUnsupportedProvider() {
		Game game = game();
		game.setExternalProvider("steam");

		assertThatThrownBy(() -> externalGameImportService.syncMetadata(game))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Unsupported external game provider");
	}

	@Test
	void syncMetadataPropagatesUpstreamError() {
		Game game = game();
		when(igdbClient.fetchById("112964"))
				.thenThrow(new ServiceUnavailableException("External game provider is unavailable"));
		when(gameRepository.save(game)).thenReturn(game);

		assertThatThrownBy(() -> externalGameImportService.syncMetadata(game))
				.isInstanceOf(ServiceUnavailableException.class)
				.hasMessage("External game provider is unavailable");

		assertThat(game.getMetadataSyncStatus()).isEqualTo(MetadataSyncStatus.FAILED);
		assertThat(game.getMetadataSyncAttemptedAt()).isNotNull();
		assertThat(game.getMetadataSyncError()).isEqualTo("External game provider is unavailable");
	}

	private Game game() {
		Game game = new Game("Hades");
		game.setExternalProvider("igdb");
		game.setExternalId("112964");
		game.setSlug("hades");
		game.setCoverUrl("https://images.example/hades.jpg");
		game.setReleaseDate(LocalDate.of(2020, 9, 17));
		return withId(game);
	}

	private Game withId(Game game) {
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}
}
