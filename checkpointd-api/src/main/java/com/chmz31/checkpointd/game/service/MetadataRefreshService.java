package com.chmz31.checkpointd.game.service;

import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.model.MetadataSyncStatus;
import com.chmz31.checkpointd.game.repository.GameRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetadataRefreshService {

	private static final Duration REFRESHING_RETRY_AFTER = Duration.ofMinutes(15);

	private final GameRepository gameRepository;
	private final ExternalGameImportService externalGameImportService;
	private final Duration staleAfter;

	public MetadataRefreshService(
			GameRepository gameRepository,
			ExternalGameImportService externalGameImportService,
			@Value("${checkpointd.metadata.refresh.stale-after-days:30}") long staleAfterDays) {
		this.gameRepository = gameRepository;
		this.externalGameImportService = externalGameImportService;
		this.staleAfter = Duration.ofDays(staleAfterDays);
	}

	public boolean isMetadataStale(Game game) {
		if (!canRefresh(game)) {
			return false;
		}
		if (game.getMetadataSyncedAt() == null) {
			return true;
		}

		return game.getMetadataSyncedAt().isBefore(Instant.now().minus(staleAfter));
	}

	public void triggerRefreshIfStale(Game game) {
		if (!isMetadataStale(game) || recentlyRefreshing(game)) {
			return;
		}

		refreshGameMetadata(game.getId());
	}

	@Async
	@Transactional
	public void refreshGameMetadata(UUID gameId) {
		gameRepository.findById(gameId).ifPresent(game -> {
			if (!isMetadataStale(game) || recentlyRefreshing(game)) {
				return;
			}

			Instant now = Instant.now();
			game.setMetadataSyncAttemptedAt(now);
			game.setMetadataSyncStatus(MetadataSyncStatus.REFRESHING);
			game.setMetadataSyncError(null);
			gameRepository.save(game);

			try {
				externalGameImportService.syncMetadata(game);
			}
			catch (RuntimeException exception) {
				game.setMetadataSyncAttemptedAt(Instant.now());
				game.setMetadataSyncStatus(MetadataSyncStatus.FAILED);
				game.setMetadataSyncError(cleanError(exception));
				gameRepository.save(game);
			}
		});
	}

	private boolean canRefresh(Game game) {
		return game.getExternalProvider() != null && !game.getExternalProvider().isBlank()
				&& game.getExternalId() != null && !game.getExternalId().isBlank();
	}

	private boolean recentlyRefreshing(Game game) {
		return game.getMetadataSyncStatus() == MetadataSyncStatus.REFRESHING
				&& game.getMetadataSyncAttemptedAt() != null
				&& game.getMetadataSyncAttemptedAt().isAfter(Instant.now().minus(REFRESHING_RETRY_AFTER));
	}

	private String cleanError(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}

		return message.length() > 500 ? message.substring(0, 500) : message;
	}
}
