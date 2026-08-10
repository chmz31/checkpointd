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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	long countByUserIdAndRatingIsNotNull(UUID userId);

	@Query("select avg(review.rating) from Review review where review.user.id = :userId and review.rating is not null")
	Double averageRatingByUserId(@Param("userId") UUID userId);

	long countByUserUsernameAndUserProfileVisibilityAndVisibilityAndRatingIsNotNull(
			String username,
			ProfileVisibility profileVisibility,
			ReviewVisibility visibility);

	@Query("""
			select avg(review.rating)
			from Review review
			where review.user.username = :username
				and review.user.profileVisibility = :profileVisibility
				and review.visibility = :visibility
				and review.rating is not null
			""")
	Double averageRatingByUsernameAndVisibility(
			@Param("username") String username,
			@Param("profileVisibility") ProfileVisibility profileVisibility,
			@Param("visibility") ReviewVisibility visibility);
}
