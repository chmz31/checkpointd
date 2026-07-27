package com.chmz31.checkpointd.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.model.MetadataSyncStatus;
import com.chmz31.checkpointd.game.repository.GameRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MetadataRefreshServiceTests {

	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Mock
	private GameRepository gameRepository;

	@Mock
	private ExternalGameImportService externalGameImportService;

	@Test
	void staleWhenExternalGameHasNeverSynced() {
		MetadataRefreshService service = service();

		assertThat(service.isMetadataStale(game())).isTrue();
	}

	@Test
	void staleWhenExternalGameSyncedBeforeThreshold() {
		MetadataRefreshService service = service();
		Game game = game();
		game.setMetadataSyncedAt(Instant.now().minusSeconds(31L * 24 * 60 * 60));

		assertThat(service.isMetadataStale(game)).isTrue();
	}

	@Test
	void notStaleWhenExternalGameSyncedRecently() {
		MetadataRefreshService service = service();
		Game game = game();
		game.setMetadataSyncedAt(Instant.now().minusSeconds(2L * 24 * 60 * 60));

		assertThat(service.isMetadataStale(game)).isFalse();
	}

	@Test
	void localGameIsNotRefreshable() {
		MetadataRefreshService service = service();

		assertThat(service.isMetadataStale(new Game("Local Game"))).isFalse();
	}

	@Test
	void triggerSkipsRecentlyRefreshingGame() {
		MetadataRefreshService service = service();
		Game game = game();
		game.setMetadataSyncStatus(MetadataSyncStatus.REFRESHING);
		game.setMetadataSyncAttemptedAt(Instant.now().minusSeconds(60));

		service.triggerRefreshIfStale(game);

		verify(gameRepository, never()).findById(any(UUID.class));
	}

	@Test
	void refreshStoresFailureStatus() {
		MetadataRefreshService service = service();
		Game game = game();
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(gameRepository.save(game)).thenReturn(game);
		when(externalGameImportService.syncMetadata(game))
				.thenThrow(new ServiceUnavailableException("External game provider is unavailable"));

		service.refreshGameMetadata(GAME_ID);

		assertThat(game.getMetadataSyncStatus()).isEqualTo(MetadataSyncStatus.FAILED);
		assertThat(game.getMetadataSyncAttemptedAt()).isNotNull();
		assertThat(game.getMetadataSyncError()).isEqualTo("External game provider is unavailable");
	}

	private MetadataRefreshService service() {
		return new MetadataRefreshService(gameRepository, externalGameImportService, 30);
	}

	private Game game() {
		Game game = new Game("Hades");
		game.setExternalProvider("igdb");
		game.setExternalId("112964");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}
}
