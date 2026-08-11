package com.chmz31.checkpointd.like.repository;

import com.chmz31.checkpointd.like.entity.ReviewLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

	boolean existsByUserIdAndReviewId(UUID userId, UUID reviewId);

	Optional<ReviewLike> findByUserIdAndReviewId(UUID userId, UUID reviewId);

	long countByReviewId(UUID reviewId);
}
