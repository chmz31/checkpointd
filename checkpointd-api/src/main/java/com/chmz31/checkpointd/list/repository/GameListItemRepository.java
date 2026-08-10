package com.chmz31.checkpointd.list.repository;

import com.chmz31.checkpointd.list.entity.GameListItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameListItemRepository extends JpaRepository<GameListItem, UUID> {

	@EntityGraph(attributePaths = {"game"})
	List<GameListItem> findByListIdOrderByPositionAsc(UUID listId);

	boolean existsByListIdAndGameId(UUID listId, UUID gameId);

	Optional<GameListItem> findByListIdAndGameId(UUID listId, UUID gameId);

	long countByListId(UUID listId);

	@Query("select coalesce(max(item.position), 0) from GameListItem item where item.list.id = :listId")
	int maxPositionByListId(@Param("listId") UUID listId);
}
