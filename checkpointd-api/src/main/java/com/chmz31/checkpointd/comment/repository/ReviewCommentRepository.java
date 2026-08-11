package com.chmz31.checkpointd.comment.repository;

import com.chmz31.checkpointd.comment.entity.ReviewComment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, UUID> {

	@EntityGraph(attributePaths = {"user"})
	Page<ReviewComment> findByReviewIdAndParentIsNullOrderByCreatedAtDesc(UUID reviewId, Pageable pageable);

	@EntityGraph(attributePaths = {"user"})
	List<ReviewComment> findByParentIdOrderByCreatedAtAsc(UUID parentId);

	@EntityGraph(attributePaths = {"user", "review"})
	Optional<ReviewComment> findByIdAndReviewId(UUID id, UUID reviewId);

	long countByReviewId(UUID reviewId);

	@Query("""
			select comment from ReviewComment comment
			join fetch comment.user
			join fetch comment.review review
			join fetch review.game
			where exists (select 1 from ReviewCommentReport r where r.comment = comment)
			order by comment.createdAt desc
			""")
	Page<ReviewComment> findReportedOrderByCreatedAtDesc(Pageable pageable);
}
