package com.chmz31.checkpointd.comment.repository;

import com.chmz31.checkpointd.comment.entity.ListCommentLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListCommentLikeRepository extends JpaRepository<ListCommentLike, UUID> {

	boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

	Optional<ListCommentLike> findByUserIdAndCommentId(UUID userId, UUID commentId);

	long countByCommentId(UUID commentId);
}
