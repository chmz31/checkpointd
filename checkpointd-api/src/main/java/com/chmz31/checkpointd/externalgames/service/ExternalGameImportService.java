package com.chmz31.checkpointd.externalgames.service;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameWebsite;
import com.chmz31.checkpointd.externalgames.dto.ImportExternalGameRequest;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.entity.GameCollections;
import com.chmz31.checkpointd.game.entity.GameWebsite;
import com.chmz31.checkpointd.game.model.MetadataSyncStatus;
import com.chmz31.checkpointd.game.repository.GameRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalGameImportService {

	private static final String IGDB_PROVIDER = "igdb";

	private final GameRepository gameRepository;
	private final IgdbClient igdbClient;

	public ExternalGameImportService(GameRepository gameRepository, IgdbClient igdbClient) {
		this.gameRepository = gameRepository;
		this.igdbClient = igdbClient;
	}

	@Transactional
	public ImportedGameResult importGame(ImportExternalGameRequest request) {
		String provider = request.provider().trim().toLowerCase(Locale.ROOT);
		String externalId = request.externalId().trim();

		validateIgdbIdentity(provider, externalId);

		Optional<Game> existing = gameRepository.findByExternalProviderAndExternalId(provider, externalId);
		if (existing.isPresent()) {
			return new ImportedGameResult(GameCollections.hydrate(existing.get()), false);
		}

		ExternalGameSearchResult externalGame = igdbClient.fetchById(externalId);
		if (externalGame == null) {
			throw new ResourceNotFoundException("External game not found");
		}

		Game game = new Game(externalGame.title());
		game.setExternalProvider(provider);
		game.setExternalId(externalId);
		applyExternalGame(game, externalGame);
		markSyncSuccess(game, Instant.now());

		try {
			return new ImportedGameResult(gameRepository.saveAndFlush(game), true);
		}
		catch (DataIntegrityViolationException exception) {
			Game raced = gameRepository.findByExternalProviderAndExternalId(provider, externalId)
					.orElseThrow(() -> exception);
			return new ImportedGameResult(GameCollections.hydrate(raced), false);
		}
	}

	public Game syncMetadata(Game game) {
		String provider = clean(game.getExternalProvider());
		String externalId = clean(game.getExternalId());
		if (provider == null || externalId == null) {
			throw new BadRequestException("Game cannot be synced without external identity");
		}

		validateIgdbIdentity(provider, externalId);

		Instant attemptedAt = Instant.now();
		game.setMetadataSyncAttemptedAt(attemptedAt);
		game.setMetadataSyncStatus(MetadataSyncStatus.REFRESHING);
		game.setMetadataSyncError(null);

		try {
			ExternalGameSearchResult externalGame = igdbClient.fetchById(externalId);
			if (externalGame == null) {
				throw new ResourceNotFoundException("External game not found");
			}

			applyExternalGame(game, externalGame);
			markSyncSuccess(game, Instant.now());

			return gameRepository.save(game);
		}
		catch (RuntimeException exception) {
			game.setMetadataSyncAttemptedAt(attemptedAt);
			game.setMetadataSyncStatus(MetadataSyncStatus.FAILED);
			game.setMetadataSyncError(cleanError(exception));
			gameRepository.save(game);
			throw exception;
		}
	}

	private void validateIgdbIdentity(String provider, String externalId) {
		if (!IGDB_PROVIDER.equals(provider)) {
			throw new BadRequestException("Unsupported external game provider");
		}
		if (!externalId.matches("\\d+")) {
			throw new BadRequestException("IGDB externalId must be numeric");
		}
	}

	private void applyExternalGame(Game game, ExternalGameSearchResult externalGame) {
		game.setTitle(externalGame.title());
		game.setSlug(externalGame.slug());
		game.setCoverUrl(externalGame.coverUrl());
		game.setReleaseDate(externalGame.releaseDate());
		game.setSummary(externalGame.summary());
		game.setGenres(externalGame.genres());
		game.setPlatforms(externalGame.platforms());
		game.setDevelopers(externalGame.developers());
		game.setPublishers(externalGame.publishers());
		game.setGameModes(externalGame.gameModes());
		game.setThemes(externalGame.themes());
		game.setPlayerPerspectives(externalGame.playerPerspectives());
		game.setWebsites(externalGame.websites().stream().map(this::toGameWebsite).toList());
		game.setExternalRating(externalGame.externalRating());
		game.setExternalRatingCount(externalGame.externalRatingCount());
		game.setScreenshotUrls(externalGame.screenshotUrls());
		game.setArtworkUrls(externalGame.artworkUrls());
		game.setBackdropUrl(externalGame.backdropUrl());
	}

	private String clean(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim().toLowerCase(Locale.ROOT);
	}

	private GameWebsite toGameWebsite(ExternalGameWebsite website) {
		return new GameWebsite(website.label(), website.url(), website.trusted());
	}

	private void markSyncSuccess(Game game, Instant syncedAt) {
		game.setMetadataSyncedAt(syncedAt);
		game.setMetadataSyncAttemptedAt(syncedAt);
		game.setMetadataSyncStatus(MetadataSyncStatus.SUCCESS);
		game.setMetadataSyncError(null);
	}

	private String cleanError(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}

		return message.length() > 500 ? message.substring(0, 500) : message;
	}
}
