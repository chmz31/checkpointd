package com.chmz31.checkpointd.game.repository;

import com.chmz31.checkpointd.game.entity.Game;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {

	Optional<Game> findByExternalProviderAndExternalId(String externalProvider, String externalId);

	boolean existsByExternalProviderAndExternalId(String externalProvider, String externalId);

	List<Game> findTop20ByTitleContainingIgnoreCaseOrderByTitleAsc(String title);

	List<Game> findTop20ByOrderByTitleAsc();
}
