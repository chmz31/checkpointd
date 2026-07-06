package com.chmz31.checkpointd.externalgames.service;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import com.chmz31.checkpointd.externalgames.dto.ImportExternalGameRequest;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ExternalGameImportService {

	private static final String IGDB_PROVIDER = "igdb";

	private final GameRepository gameRepository;
	private final IgdbClient igdbClient;

	public ExternalGameImportService(GameRepository gameRepository, IgdbClient igdbClient) {
		this.gameRepository = gameRepository;
		this.igdbClient = igdbClient;
	}

	public ImportedGameResult importGame(ImportExternalGameRequest request) {
		String provider = request.provider().trim().toLowerCase(Locale.ROOT);
		String externalId = request.externalId().trim();

		validateIgdbIdentity(provider, externalId);

		Optional<Game> existing = gameRepository.findByExternalProviderAndExternalId(provider, externalId);
		if (existing.isPresent()) {
			return new ImportedGameResult(existing.get(), false);
		}

		ExternalGameSearchResult externalGame = igdbClient.fetchById(externalId);
		if (externalGame == null) {
			throw new ResourceNotFoundException("External game not found");
		}

		Game game = new Game(externalGame.title());
		game.setExternalProvider(provider);
		game.setExternalId(externalId);
		applyExternalGame(game, externalGame);

		try {
			return new ImportedGameResult(gameRepository.saveAndFlush(game), true);
		}
		catch (DataIntegrityViolationException exception) {
			return new ImportedGameResult(gameRepository.findByExternalProviderAndExternalId(provider, externalId)
					.orElseThrow(() -> exception), false);
		}
	}

	public Game syncMetadata(Game game) {
		String provider = clean(game.getExternalProvider());
		String externalId = clean(game.getExternalId());
		if (provider == null || externalId == null) {
			throw new BadRequestException("Game cannot be synced without external identity");
		}

		validateIgdbIdentity(provider, externalId);

		ExternalGameSearchResult externalGame = igdbClient.fetchById(externalId);
		if (externalGame == null) {
			throw new ResourceNotFoundException("External game not found");
		}

		applyExternalGame(game, externalGame);

		return gameRepository.save(game);
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
	}

	private String clean(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim().toLowerCase(Locale.ROOT);
	}
}
