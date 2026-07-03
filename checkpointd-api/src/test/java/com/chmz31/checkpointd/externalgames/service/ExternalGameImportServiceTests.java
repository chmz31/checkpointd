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
import com.chmz31.checkpointd.externalgames.dto.ImportExternalGameRequest;
import com.chmz31.checkpointd.game.entity.Game;
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
				List.of("PC", "Switch")));
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
