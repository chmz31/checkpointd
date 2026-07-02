package com.chmz31.checkpointd.game.repository;

import com.chmz31.checkpointd.game.entity.Game;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {
}
