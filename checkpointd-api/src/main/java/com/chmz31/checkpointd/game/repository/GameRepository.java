package com.chmz31.checkpointd.game.repository;

import com.chmz31.checkpointd.game.entity.Game;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {

	// Collections are intentionally NOT joined here (see GameCollections) — joining all of them
	// in one query multiplies rows combinatorially and can exhaust heap for metadata-rich games.
	Optional<Game> findByExternalProviderAndExternalId(String externalProvider, String externalId);

	boolean existsByExternalProviderAndExternalId(String externalProvider, String externalId);

	List<Game> findTop20ByTitleContainingIgnoreCaseOrderByTitleAsc(String title);

	List<Game> findTop20ByOrderByTitleAsc();
}
