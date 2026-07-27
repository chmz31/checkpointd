package com.chmz31.checkpointd.game.repository;

import com.chmz31.checkpointd.game.entity.Game;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {

	@EntityGraph(attributePaths = {
			"genres", "platforms", "developers", "publishers", "gameModes", "themes",
			"playerPerspectives", "websites", "screenshotUrls", "artworkUrls"
	})
	Optional<Game> findByExternalProviderAndExternalId(String externalProvider, String externalId);

	boolean existsByExternalProviderAndExternalId(String externalProvider, String externalId);

	@EntityGraph(attributePaths = {
			"genres", "platforms", "developers", "publishers", "gameModes", "themes",
			"playerPerspectives", "websites", "screenshotUrls", "artworkUrls"
	})
	List<Game> findTop20ByTitleContainingIgnoreCaseOrderByTitleAsc(String title);

	@EntityGraph(attributePaths = {
			"genres", "platforms", "developers", "publishers", "gameModes", "themes",
			"playerPerspectives", "websites", "screenshotUrls", "artworkUrls"
	})
	List<Game> findTop20ByOrderByTitleAsc();

	@Override
	@EntityGraph(attributePaths = {
			"genres", "platforms", "developers", "publishers", "gameModes", "themes",
			"playerPerspectives", "websites", "screenshotUrls", "artworkUrls"
	})
	Optional<Game> findById(UUID id);
}
