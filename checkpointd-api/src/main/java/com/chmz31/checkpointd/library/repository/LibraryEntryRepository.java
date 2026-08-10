package com.chmz31.checkpointd.library.repository;

import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, UUID> {

	boolean existsByUserIdAndGameId(UUID userId, UUID gameId);

	long countByUserId(UUID userId);

	long countByUserIdAndStatus(UUID userId, LibraryStatus status);

	@EntityGraph(attributePaths = {"game"})
	Optional<LibraryEntry> findByIdAndUserId(UUID id, UUID userId);

	@EntityGraph(attributePaths = {"game"})
	Optional<LibraryEntry> findByUserIdAndGameId(UUID userId, UUID gameId);

	@Query(
			value = """
					select entry
					from LibraryEntry entry
					join entry.game game
					where entry.user.id = :userId
						and (:status is null or entry.status = :status)
					""",
			countQuery = """
					select count(entry)
					from LibraryEntry entry
					join entry.game game
					where entry.user.id = :userId
						and (:status is null or entry.status = :status)
					""")
	@EntityGraph(attributePaths = {"game"})
	Page<LibraryEntry> findUserLibrary(
			@Param("userId") UUID userId,
			@Param("status") LibraryStatus status,
			Pageable pageable);

	@Query(
			value = """
					select entry
					from LibraryEntry entry
					join entry.game game
					where entry.user.id = :userId
						and (:status is null or entry.status = :status)
						and (lower(entry.game.title) like :pattern
							or lower(coalesce(entry.notes, '')) like :pattern)
					""",
			countQuery = """
					select count(entry)
					from LibraryEntry entry
					join entry.game game
					where entry.user.id = :userId
						and (:status is null or entry.status = :status)
						and (lower(entry.game.title) like :pattern
							or lower(coalesce(entry.notes, '')) like :pattern)
					""")
	@EntityGraph(attributePaths = {"game"})
	Page<LibraryEntry> searchUserLibrary(
			@Param("userId") UUID userId,
			@Param("status") LibraryStatus status,
			@Param("pattern") String pattern,
			Pageable pageable);

	@EntityGraph(attributePaths = {"game", "game.genres", "game.platforms"})
	List<LibraryEntry> findTop8ByUserIdOrderByUpdatedAtDesc(UUID userId);
}
