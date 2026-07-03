package com.chmz31.checkpointd.game.service;

import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.dto.CreateGameRequest;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

	private final GameRepository gameRepository;

	public GameService(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@Transactional
	public Game create(CreateGameRequest request) {
		String externalProvider = cleanOptional(request.externalProvider());
		if (externalProvider != null) {
			externalProvider = externalProvider.toLowerCase();
		}
		String externalId = cleanOptional(request.externalId());
		validateExternalIdentity(externalProvider, externalId);

		if (externalProvider != null
				&& gameRepository.existsByExternalProviderAndExternalId(externalProvider, externalId)) {
			throw new DuplicateResourceException("Game external identity already exists");
		}

		Game game = new Game(request.title().trim());
		game.setExternalProvider(externalProvider);
		game.setExternalId(externalId);
		game.setSlug(cleanOptional(request.slug()));
		game.setCoverUrl(cleanOptional(request.coverUrl()));
		game.setReleaseDate(request.releaseDate());
		game.setSummary(cleanOptional(request.summary()));
		game.setGenres(request.genres());
		game.setPlatforms(request.platforms());

		return gameRepository.save(game);
	}

	@Transactional(readOnly = true)
	public List<Game> list(String query) {
		String cleanQuery = cleanOptional(query);

		if (cleanQuery == null) {
			return gameRepository.findTop20ByOrderByTitleAsc();
		}

		return gameRepository.findTop20ByTitleContainingIgnoreCaseOrderByTitleAsc(cleanQuery);
	}

	@Transactional(readOnly = true)
	public Game get(UUID gameId) {
		return gameRepository.findById(gameId)
				.orElseThrow(() -> new ResourceNotFoundException("Game not found"));
	}

	private void validateExternalIdentity(String externalProvider, String externalId) {
		if ((externalProvider == null && externalId != null) || (externalProvider != null && externalId == null)) {
			throw new BadRequestException("externalProvider and externalId must be provided together");
		}
	}

	private String cleanOptional(String value) {
		if (value == null) {
			return null;
		}
		String cleaned = value.trim();

		return cleaned.isEmpty() ? null : cleaned;
	}
}
