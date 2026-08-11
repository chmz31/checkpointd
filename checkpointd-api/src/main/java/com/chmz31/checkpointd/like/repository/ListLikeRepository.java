package com.chmz31.checkpointd.like.repository;

import com.chmz31.checkpointd.like.entity.ListLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListLikeRepository extends JpaRepository<ListLike, UUID> {

	boolean existsByUserIdAndListId(UUID userId, UUID listId);

	Optional<ListLike> findByUserIdAndListId(UUID userId, UUID listId);

	long countByListId(UUID listId);
}
