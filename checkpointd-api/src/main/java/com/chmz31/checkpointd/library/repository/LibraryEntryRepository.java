package com.chmz31.checkpointd.library.repository;

import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, UUID> {

	boolean existsByUserIdAndGameId(UUID userId, UUID gameId);

	@EntityGraph(attributePaths = "game")
	Optional<LibraryEntry> findByIdAndUserId(UUID id, UUID userId);

	@EntityGraph(attributePaths = "game")
	List<LibraryEntry> findTop50ByUserIdOrderByUpdatedAtDesc(UUID userId);

	@EntityGraph(attributePaths = "game")
	List<LibraryEntry> findTop50ByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, LibraryStatus status);
}
