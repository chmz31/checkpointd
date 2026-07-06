package com.chmz31.checkpointd.library.repository;

import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, UUID> {

	boolean existsByUserIdAndGameId(UUID userId, UUID gameId);

	long countByUserId(UUID userId);

	long countByUserIdAndStatus(UUID userId, LibraryStatus status);

	long countByUserIdAndRatingIsNotNull(UUID userId);

	@Query("select avg(entry.rating) from LibraryEntry entry where entry.user.id = :userId and entry.rating is not null")
	Double averageRatingByUserId(@Param("userId") UUID userId);

	@EntityGraph(attributePaths = {"game", "game.genres", "game.platforms", "game.screenshotUrls", "game.artworkUrls"})
	Optional<LibraryEntry> findByIdAndUserId(UUID id, UUID userId);

	@EntityGraph(attributePaths = {"game", "game.genres", "game.platforms", "game.screenshotUrls", "game.artworkUrls"})
	List<LibraryEntry> findTop50ByUserIdOrderByUpdatedAtDesc(UUID userId);

	@EntityGraph(attributePaths = {"game", "game.genres", "game.platforms", "game.screenshotUrls", "game.artworkUrls"})
	List<LibraryEntry> findTop50ByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, LibraryStatus status);
}
