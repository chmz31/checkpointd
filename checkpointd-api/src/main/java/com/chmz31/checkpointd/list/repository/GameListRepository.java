package com.chmz31.checkpointd.list.repository;

import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameListRepository extends JpaRepository<GameList, UUID> {

	@EntityGraph(attributePaths = {"user"})
	List<GameList> findAllById(Iterable<UUID> ids);

	@EntityGraph(attributePaths = {"user"})
	Optional<GameList> findByIdAndUserId(UUID id, UUID userId);

	@EntityGraph(attributePaths = {"user"})
	Optional<GameList> findByIdAndVisibilityAndUserProfileVisibility(
			UUID id,
			ListVisibility visibility,
			ProfileVisibility profileVisibility);

	@EntityGraph(attributePaths = {"user"})
	Optional<GameList> findByIdAndUserUsernameAndVisibilityAndUserProfileVisibility(
			UUID id,
			String username,
			ListVisibility visibility,
			ProfileVisibility profileVisibility);

	@EntityGraph(attributePaths = {"user"})
	Page<GameList> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

	@EntityGraph(attributePaths = {"user"})
	Page<GameList> findByUserUsernameAndUserProfileVisibilityAndVisibilityOrderByUpdatedAtDesc(
			String username,
			ProfileVisibility profileVisibility,
			ListVisibility visibility,
			Pageable pageable);
}
