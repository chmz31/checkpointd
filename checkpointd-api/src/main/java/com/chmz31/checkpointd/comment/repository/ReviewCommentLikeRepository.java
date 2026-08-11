package com.chmz31.checkpointd.comment.repository;

import com.chmz31.checkpointd.comment.entity.ReviewCommentLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentLikeRepository extends JpaRepository<ReviewCommentLike, UUID> {

	boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

	Optional<ReviewCommentLike> findByUserIdAndCommentId(UUID userId, UUID commentId);

	long countByCommentId(UUID commentId);
}
