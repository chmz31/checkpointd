package com.chmz31.checkpointd.review.repository;

import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

	@EntityGraph(attributePaths = {"user", "game"})
	Optional<Review> findByUserIdAndGameId(UUID userId, UUID gameId);

	@EntityGraph(attributePaths = {"user", "game"})
	Page<Review> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "game"})
	Optional<Review> findByUserUsernameAndGameIdAndVisibilityAndUserProfileVisibility(
			String username,
			UUID gameId,
			ReviewVisibility visibility,
			ProfileVisibility profileVisibility);

	@EntityGraph(attributePaths = {"user", "game"})
	Page<Review> findByGameIdAndVisibilityAndUserProfileVisibilityOrderByUpdatedAtDesc(
			UUID gameId,
			ReviewVisibility visibility,
			ProfileVisibility profileVisibility,
			Pageable pageable);

	@EntityGraph(attributePaths = {"user", "game"})
	Page<Review> findByUserUsernameAndUserProfileVisibilityAndVisibilityOrderByUpdatedAtDesc(
			String username,
			ProfileVisibility profileVisibility,
			ReviewVisibility visibility,
			Pageable pageable);

	long countByUserUsernameAndUserProfileVisibilityAndVisibility(
			String username,
			ProfileVisibility profileVisibility,
			ReviewVisibility visibility);
}
