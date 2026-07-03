package com.chmz31.checkpointd.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.dto.CreateGameRequest;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameServiceTests {

	@Mock
	private GameRepository gameRepository;

	@InjectMocks
	private GameService gameService;

	@Test
	void createSavesGame() {
		CreateGameRequest request = new CreateGameRequest(
				" IGDB ", " 123 ", " Chrono Trigger ", " chrono-trigger ", " https://img.example/cover.jpg ",
				LocalDate.of(1995, 3, 11), " Time travel RPG ", List.of(" RPG ", "Adventure"),
				List.of(" SNES ", "PC"));

		when(gameRepository.existsByExternalProviderAndExternalId("igdb", "123")).thenReturn(false);
		when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Game created = gameService.create(request);

		ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
		verify(gameRepository).existsByExternalProviderAndExternalId("igdb", "123");
		verify(gameRepository).save(gameCaptor.capture());
		Game saved = gameCaptor.getValue();

		assertThat(created).isSameAs(saved);
		assertThat(saved.getExternalProvider()).isEqualTo("igdb");
		assertThat(saved.getExternalId()).isEqualTo("123");
		assertThat(saved.getTitle()).isEqualTo("Chrono Trigger");
		assertThat(saved.getSlug()).isEqualTo("chrono-trigger");
		assertThat(saved.getCoverUrl()).isEqualTo("https://img.example/cover.jpg");
		assertThat(saved.getReleaseDate()).isEqualTo(LocalDate.of(1995, 3, 11));
		assertThat(saved.getSummary()).isEqualTo("Time travel RPG");
		assertThat(saved.getGenres()).containsExactly("RPG", "Adventure");
		assertThat(saved.getPlatforms()).containsExactly("SNES", "PC");
	}

	@Test
	void createRejectsPartialExternalIdentity() {
		CreateGameRequest request = new CreateGameRequest("igdb", null, "Chrono Trigger", null, null, null);

		assertThatThrownBy(() -> gameService.create(request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("externalProvider and externalId must be provided together");

		verify(gameRepository, never()).save(any(Game.class));
	}

	@Test
	void createRejectsDuplicateExternalIdentity() {
		CreateGameRequest request = new CreateGameRequest("IGDB", " 123 ", "Chrono Trigger", null, null, null);

		when(gameRepository.existsByExternalProviderAndExternalId("igdb", "123")).thenReturn(true);

		assertThatThrownBy(() -> gameService.create(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Game external identity already exists");

		verify(gameRepository).existsByExternalProviderAndExternalId("igdb", "123");
		verify(gameRepository, never()).save(any(Game.class));
	}

	@Test
	void listSearchesByTitleWhenQueryIsPresent() {
		List<Game> games = List.of(new Game("Chrono Trigger"));

		when(gameRepository.findTop20ByTitleContainingIgnoreCaseOrderByTitleAsc("chrono")).thenReturn(games);

		assertThat(gameService.list(" chrono ")).isSameAs(games);
	}

	@Test
	void listReturnsDefaultWhenQueryIsAbsent() {
		List<Game> games = List.of(new Game("Chrono Trigger"));

		when(gameRepository.findTop20ByOrderByTitleAsc()).thenReturn(games);

		assertThat(gameService.list(" ")).isSameAs(games);
	}

	@Test
	void getRejectsMissingGame() {
		UUID gameId = UUID.fromString("00000000-0000-0000-0000-000000000001");

		when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameService.get(gameId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Game not found");
	}
}
