package com.chmz31.checkpointd.follow.repository;

import com.chmz31.checkpointd.follow.entity.Follow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

	boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

	Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

	long countByFollowerId(UUID followerId);

	long countByFolloweeId(UUID followeeId);

	@EntityGraph(attributePaths = {"followee"})
	Page<Follow> findByFollowerIdOrderByCreatedAtDesc(UUID followerId, Pageable pageable);

	@EntityGraph(attributePaths = {"follower"})
	Page<Follow> findByFolloweeIdOrderByCreatedAtDesc(UUID followeeId, Pageable pageable);
}
