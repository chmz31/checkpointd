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

	@Mock
	private MetadataRefreshService self;

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
	void triggerDispatchesRefreshWhenClaimSucceeds() {
		MetadataRefreshService service = service();
		Game game = game();
		when(self.claimRefresh(GAME_ID)).thenReturn(GAME_ID);

		service.triggerRefreshIfStale(game);

		verify(self).refreshGameMetadata(GAME_ID);
	}

	@Test
	void triggerDoesNothingWhenClaimFails() {
		MetadataRefreshService service = service();
		Game game = game();
		when(self.claimRefresh(GAME_ID)).thenReturn(null);

		service.triggerRefreshIfStale(game);

		verify(self, never()).refreshGameMetadata(any(UUID.class));
	}

	@Test
	void claimRefreshSkipsRecentlyRefreshingGame() {
		MetadataRefreshService service = service();
		Game game = game();
		game.setMetadataSyncStatus(MetadataSyncStatus.REFRESHING);
		game.setMetadataSyncAttemptedAt(Instant.now().minusSeconds(60));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

		assertThat(service.claimRefresh(GAME_ID)).isNull();
		verify(gameRepository, never()).save(any(Game.class));
	}

	@Test
	void claimRefreshSkipsNotStaleGame() {
		MetadataRefreshService service = service();
		Game game = game();
		game.setMetadataSyncedAt(Instant.now().minusSeconds(2L * 24 * 60 * 60));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

		assertThat(service.claimRefresh(GAME_ID)).isNull();
		verify(gameRepository, never()).save(any(Game.class));
	}

	@Test
	void claimRefreshMarksGameRefreshingAndReturnsId() {
		MetadataRefreshService service = service();
		Game game = game();
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(gameRepository.save(game)).thenReturn(game);

		assertThat(service.claimRefresh(GAME_ID)).isEqualTo(GAME_ID);
		assertThat(game.getMetadataSyncStatus()).isEqualTo(MetadataSyncStatus.REFRESHING);
		assertThat(game.getMetadataSyncAttemptedAt()).isNotNull();
	}

	@Test
	void claimRefreshReturnsNullWhenGameMissing() {
		MetadataRefreshService service = service();
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

		assertThat(service.claimRefresh(GAME_ID)).isNull();
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
		return new MetadataRefreshService(gameRepository, externalGameImportService, 30, self);
	}

	private Game game() {
		Game game = new Game("Hades");
		game.setExternalProvider("igdb");
		game.setExternalId("112964");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}
}
